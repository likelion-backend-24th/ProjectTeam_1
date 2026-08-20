package com.team1.cityfarm.repository;

import com.team1.cityfarm.entity.Subscription;
import com.team1.cityfarm.entity.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository
        extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findByUserId(Long userId);

    Optional<Subscription> findByUserIdAndStatus(
            Long userId,
            SubscriptionStatus status
    );

    boolean existsByUserIdAndStatus(
            Long userId,
            SubscriptionStatus status
    );

    // 구독 만료 배치용 — 해지 예약됐고 현재 기간이 이미 끝난 구독 조회
    List<Subscription> findByStatusAndCancelAtPeriodEndTrueAndCurrentPeriodEndBefore(
            SubscriptionStatus status,
            LocalDateTime time
    );
}
