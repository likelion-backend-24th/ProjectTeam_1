package com.team1.cityfarm.service;

import com.team1.cityfarm.dto.PassResponseDto;
import com.team1.cityfarm.dto.SubscriptionCreateRequestDto;
import com.team1.cityfarm.dto.SubscriptionPassDto;
import com.team1.cityfarm.dto.SubscriptionResponseDto;
import com.team1.cityfarm.entity.*;
import com.team1.cityfarm.global.exception.CustomError;
import com.team1.cityfarm.global.exception.CustomException;
import com.team1.cityfarm.repository.BillingKeyRepository;
import com.team1.cityfarm.repository.SubscriptionPassRepository;
import com.team1.cityfarm.repository.SubscriptionRepository;
import com.team1.cityfarm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final SubscriptionPassRepository subscriptionPassRepository;
    private final PortOnePaymentService portOnePaymentService; // 포트원 결제 전용 서비스

    /**
     * 1. 내 활성 구독 정보 조회
     */
    @Transactional(readOnly = true)
    public SubscriptionResponseDto getMyActiveSubscription(Long userId) {
        Subscription subscription = subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE)
                .orElseThrow(() -> new IllegalArgumentException("활성화된 구독이 없습니다."));

        return SubscriptionResponseDto.from(subscription);
    }

    /**
     * 2. 정기 구독 신청 (빌링키 결제 및 수강권 발급)
     */
    @Transactional
    public SubscriptionResponseDto createSubscription(Long userId, SubscriptionCreateRequestDto request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));

        // 이미 활성화된 구독이 있는지 검증
        if (subscriptionRepository.existsByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE)) {
            throw new IllegalStateException("이미 활성화된 정기 구독이 존재합니다.");
        }

        // 요금제(PlanType)에 따른 결제 금액 설정
        int price = calculatePrice(request.getPlanType());
        String orderName = request.getPlanType().name() + " 정기구독";

        // 고유 결제 번호(paymentId) 생성
        String paymentId = "sub_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        // 포트원 빌링키 결제 요청 (실패 시 RuntimeException 발생 및 트랜잭션 롤백)
        portOnePaymentService.payWithBillingKey(request.getBillingKey(), paymentId, price, orderName, user.getId());

        // 결제 성공 시 구독(Subscription) 엔티티 생성
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneMonthLater = now.plusMonths(1);

        Subscription subscription = Subscription.builder()
                .user(user)
                .planType(request.getPlanType())
                .status(SubscriptionStatus.ACTIVE)
                .currentPeriodStart(now)
                .currentPeriodEnd(oneMonthLater)
                .cancelAtPeriodEnd(false)
                .build();

        subscriptionRepository.save(subscription);

        // 구독에 매핑되는 수강권(SubscriptionPass) 엔티티 발급
        int passCount = getPassCount(request.getPlanType());

        SubscriptionPass pass = SubscriptionPass.builder()
                .subscription(subscription)
                .totalCount(passCount)
                .remainingCount(passCount)
                .validFrom(now)
                .validUntil(oneMonthLater)
                .status(PassStatus.ACTIVE)
                .build();

        subscriptionPassRepository.save(pass);

        return SubscriptionResponseDto.from(subscription);
    }

    /**
     * 3. 정기 구독 해지 예약 (다음 결제일에 갱신 방지)
     */
    @Transactional
    public void cancelSubscriptionAtPeriodEnd(Long userId, Long subscriptionId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("구독 정보를 찾을 수 없습니다."));

        // 본인 구독권인지 검증
        if (!subscription.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("권한이 없습니다.");
        }

        // 다음 주기에 해지되도록 상태 변경
        subscription.setCancelAtPeriodEnd(true);
    }

    /**
     * 4. 특정 구독권의 패스(이용권) 단건 조회
     */
    @Transactional(readOnly = true)
    public PassResponseDto getSubscriptionPass(Long userId, Long subscriptionId) {
        SubscriptionPass pass = subscriptionPassRepository.findBySubscriptionId(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("해당 구독의 수강권 정보를 찾을 수 없습니다."));

        // 본인의 수강권인지 검증
        if (!pass.getSubscription().getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("수강권 조회 권한이 없습니다.");
        }

        return PassResponseDto.from(pass);
    }

    // ==========================================
    // 헬퍼 메서드 (비즈니스 요구사항에 맞게 수정)
    // ==========================================

    /**
     * PlanType에 따른 결제 금액 반환
     */
    private int calculatePrice(SubscriptionPlanType planType) {
        if (planType == null) return 0;

        switch (planType) {
            // 예시: (프로젝트 설정에 맞춰 주석 해제 및 수정해주세요)
             case BASIC: return 30000;
            // case PREMIUM: return 20000;
            default: return 30000; // 테스트용 금액 100원
        }
    }

    /**
     * PlanType에 따른 지급 수강권 횟수 반환
     */
    private int getPassCount(SubscriptionPlanType planType) {
        if (planType == null) return 0;

        switch (planType) {
             case BASIC: return 3;
            // case PREMIUM: return 3;
            default: return 3;
        }
    }
}