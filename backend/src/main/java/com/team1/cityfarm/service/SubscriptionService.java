package com.team1.cityfarm.service;

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

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionPassRepository subscriptionPassRepository;
    private final BillingKeyRepository billingKeyRepository;
    private final UserRepository userRepository;


    //내 활성 구독 정보 조회
    public SubscriptionResponseDto getMyActiveSubscription(Long userId) {
        Subscription subscription = subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE)
                .orElseThrow(() -> new CustomException(CustomError.SUBSCRIPTION_NOT_FOUND));

        return SubscriptionResponseDto.from(subscription);
    }


    //정기 구독 생성 / 최초 결제
    @Transactional
    public SubscriptionResponseDto createSubscription(Long userId, SubscriptionCreateRequestDto request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(CustomError.USER_NOT_FOUND));

        // 1. 등록된 빌링키 검증
        billingKeyRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(CustomError.BILLING_KEY_NOT_FOUND));

        // 2. 이미 활성화된 구독이 있는지 중복 체크
        if (subscriptionRepository.existsByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE)) {
            throw new CustomException(CustomError.DUPLICATE_SUBSCRIPTION);
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime periodEnd = now.plusMonths(1);

        // 3. Subscription 엔티티 생성
        Subscription subscription = Subscription.builder()
                .user(user)
                .planType(request.getPlanType() != null ? request.getPlanType() : SubscriptionPlanType.BASIC)
                .status(SubscriptionStatus.ACTIVE)
                .currentPeriodStart(now)
                .currentPeriodEnd(periodEnd)
                .cancelAtPeriodEnd(false)
                .build();

        Subscription savedSubscription = subscriptionRepository.save(subscription);

        // 4. 구독 수강권(Pass) 단건 생성 및 지급 (기본 3회 설정)
        generateMonthlyPass(savedSubscription, 3);

        log.info("[구독 생성 완료] userId: {}, subscriptionId: {}", userId, savedSubscription.getId());

        return SubscriptionResponseDto.from(savedSubscription);
    }

    //구독 해지 예약
    @Transactional
    public SubscriptionResponseDto cancelAtPeriodEnd(Long subscriptionId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new CustomException(CustomError.SUBSCRIPTION_NOT_FOUND));

        subscription.setCancelAtPeriodEnd(true);
        subscription.setCancelledAt(LocalDateTime.now());

        log.info("[구독 만료 해지 예약 완료] subscriptionId: {}", subscriptionId);

        return SubscriptionResponseDto.from(subscription);
    }


    //구독 보유 수강권 단건 조회
    public SubscriptionPassDto getPass(Long subscriptionId) {
        SubscriptionPass pass = subscriptionPassRepository.findBySubscriptionId(subscriptionId)
                .orElseThrow(() -> new CustomException(CustomError.SUBSCRIPTION_PASS_NOT_FOUND));

        return SubscriptionPassDto.from(pass);
    }


    //수강권 단건 생성
    private void generateMonthlyPass(Subscription subscription, int totalPassCount) {
        SubscriptionPass pass = SubscriptionPass.builder()
                .subscription(subscription)
                .totalCount(totalPassCount)
                .remainingCount(totalPassCount)
                .validFrom(subscription.getCurrentPeriodStart())
                .validUntil(subscription.getCurrentPeriodEnd())
                .status(PassStatus.ACTIVE)
                .build();

        subscriptionPassRepository.save(pass);
    }
}