package com.team1.cityfarm.service;

import com.team1.cityfarm.dto.BillingKeyIntentResponseDto;
import com.team1.cityfarm.dto.BillingKeyResponseDto;
import com.team1.cityfarm.entity.BillingKey;
import com.team1.cityfarm.entity.BillingKeyStatus;
import com.team1.cityfarm.entity.User;
import com.team1.cityfarm.global.exception.CustomError;
import com.team1.cityfarm.global.exception.CustomException;
import com.team1.cityfarm.portone.PortoneBillingKeyResponseDto;
import com.team1.cityfarm.portone.PortonePaymentClient;
import com.team1.cityfarm.repository.BillingKeyRepository;
import com.team1.cityfarm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BillingKeyService {

    private final BillingKeyRepository billingKeyRepository;
    private final UserRepository userRepository;
    private final PortonePaymentClient portonePaymentClient;
    private final SubscriptionService subscriptionService;

    /**
     * [내 빌링키 조회]
     * 빌링키가 없으면 404 CustomException을 발생시킵니다.
     */
    public BillingKeyResponseDto getMyActiveBillingKey(Long userId) {
        BillingKey billingKey = billingKeyRepository.findByUserIdAndStatus(userId, BillingKeyStatus.ACTIVE)
                .orElseThrow(() -> new CustomException(CustomError.BILLING_KEY_NOT_FOUND));

        return BillingKeyResponseDto.from(billingKey);
    }

    /**
     * [빌링키 발급 의도(Intent) 생성]
     */
    public BillingKeyIntentResponseDto createIssuanceIntent(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(CustomError.USER_NOT_FOUND));

        String issueId = "issue_" + UUID.randomUUID().toString().substring(0, 8);
        String customerId = "user_" + user.getId();

        return BillingKeyIntentResponseDto.builder()
                .issueId(issueId)
                .customerId(customerId)
                .build();
    }

    /**
     * [빌링키 발급 확정 및 저장]
     * 프론트엔드(PortOne SDK)가 방금 발급받은 빌링키 값을 그대로 신뢰하지 않고,
     * PortOne API로 해당 빌링키를 단건 조회해 실제로 발급(ISSUED)된 값인지 검증한 뒤 저장한다.
     */
    @Transactional
    public BillingKeyResponseDto confirmIssuance(String issueId, String billingKey, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(CustomError.USER_NOT_FOUND));

        PortoneBillingKeyResponseDto portOneBillingKey = portonePaymentClient.getBillingKeyDetails(billingKey);

        if (!"ISSUED".equalsIgnoreCase(portOneBillingKey.getStatus())
                || !billingKey.equals(portOneBillingKey.getBillingKey())) {
            log.error("[빌링키 검증 실패] userId: {}, issueId: {}, status: {}", userId, issueId, portOneBillingKey.getStatus());
            throw new CustomException(CustomError.BILLING_KEY_VERIFY_FAILED);
        }

        // 새 키를 저장하기 전에 기존 ACTIVE 키를 먼저 확보해둔다.
        BillingKey oldKey = billingKeyRepository.findByUserIdAndStatus(userId, BillingKeyStatus.ACTIVE).orElse(null);

        LocalDateTime now = LocalDateTime.now();

        // BillingKey 엔티티 생성 (PortOne이 실제로 발급한 빌링키 값을 저장)
        BillingKey entity = BillingKey.builder()
                .user(user)
                .billingKeyEncrypted(portOneBillingKey.getBillingKey()) // 실제 암호화 모듈 연동 가능
                .status(BillingKeyStatus.ACTIVE)
                .issuedAt(now)
                .expiredAt(now.plusYears(3)) // 기본 3년 유효기간 예시
                .build();

        BillingKey savedKey = billingKeyRepository.save(entity);

        if (oldKey != null) {
            // 1. 이전 키에 예약이 걸려있으면 새 키로 마이그레이션 (실패하면 여기서 예외 -> 전체 롤백,
            //    새 카드 저장도 함께 취소되어 구독이 조용히 끊기는 상황을 막는다)
            subscriptionService.migrateActiveScheduleToNewBillingKey(userId, savedKey);

            // 2. 이제 아무 것도 참조하지 않는 이전 키를 PortOne에서 정리 — 실패해도 트랜잭션은 유지
            try {
                portonePaymentClient.deleteBillingKey(oldKey.getBillingKeyEncrypted(), "카드 변경으로 인한 이전 빌링키 해지");
            } catch (Exception e) {
                log.error("[이전 빌링키 해지 실패 - 무시하고 진행] userId: {}, oldBillingKeyId: {}", userId, oldKey.getId(), e);
            }
            // 하드 삭제 대신 REVOKED로 남긴다(이력 보존). PortOne 쪽에서는 이미 해지됐으므로
            // 이 row의 billingKeyEncrypted 값을 다시 결제/예약 요청에 재사용하면 안 된다 — 항상
            // findByUserIdAndStatus(ACTIVE)로만 "현재 쓰는 키"를 조회할 것.
            oldKey.updateBillingKeyStatus(BillingKeyStatus.REVOKED);
        }

        log.info("[빌링키 등록 완료] userId: {}, billingKeyId: {}", userId, savedKey.getId());

        return BillingKeyResponseDto.from(savedKey);
    }

    /**
     * 활성 빌링키 존재 여부만 확인한다 (예외 없이). 호출부에서 revokeMyBillingKey를
     * 예외 없이 안전하게 호출할지 미리 판단할 때 사용 - BILLING_KEY_NOT_FOUND를
     * 던지고 잡는 방식은 같은 트랜잭션 안에서 rollback-only를 유발해 위험하다.
     */
    public boolean hasActiveBillingKey(Long userId) {
        return billingKeyRepository.findByUserIdAndStatus(userId, BillingKeyStatus.ACTIVE).isPresent();
    }

    /**
     * [빌링키 단독 삭제 (카드 교체 없이 그냥 삭제)]
     * 활성 구독에 아직 실행되지 않은 예약결제가 걸려있으면 삭제를 막는다 — 대체 카드 없이
     * 지우면 다음 회차 결제 수단이 사라져서 구독이 조용히 끊긴다. 하드 삭제 대신 REVOKED로 남긴다.
     */
    @Transactional
    public void revokeMyBillingKey(Long userId, String reason) {
        BillingKey activeKey = billingKeyRepository.findByUserIdAndStatus(userId, BillingKeyStatus.ACTIVE)
                .orElseThrow(() -> new CustomException(CustomError.BILLING_KEY_NOT_FOUND));

        if (subscriptionService.hasActiveScheduledPayment(userId)) {
            throw new CustomException(CustomError.BILLING_KEY_IN_USE);
        }

        String revokeReason = (reason != null && !reason.isBlank()) ? reason : "사용자 요청에 의한 카드 삭제";

        portonePaymentClient.deleteBillingKey(activeKey.getBillingKeyEncrypted(), revokeReason);
        activeKey.updateBillingKeyStatus(BillingKeyStatus.REVOKED);

        log.info("[빌링키 삭제 완료] userId: {}, billingKeyId: {}", userId, activeKey.getId());
    }
}
