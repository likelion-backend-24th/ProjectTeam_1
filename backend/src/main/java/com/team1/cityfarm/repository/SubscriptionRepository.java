package com.team1.cityfarm.repository;

import com.team1.cityfarm.entity.Subscription;
import com.team1.cityfarm.entity.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
