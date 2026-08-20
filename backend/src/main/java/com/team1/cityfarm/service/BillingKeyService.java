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

    /**
     * [내 빌링키 조회]
     * 빌링키가 없으면 404 CustomException을 발생시킵니다.
     */
    public BillingKeyResponseDto getMyActiveBillingKey(Long userId) {
        BillingKey billingKey = billingKeyRepository.findByUserId(userId)
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

        // 기존 빌링키가 존재할 경우 삭제 또는 교체 처리
        billingKeyRepository.findByUserId(userId)
                .ifPresent(billingKeyRepository::delete);

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
        log.info("[빌링키 등록 완료] userId: {}, billingKeyId: {}", userId, savedKey.getId());

        return BillingKeyResponseDto.from(savedKey);
    }
}
