package com.team1.cityfarm.repository;

import com.team1.cityfarm.entity.SubscriptionPass;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SubscriptionPassRepository
        extends JpaRepository<SubscriptionPass, Long> {

    Optional<SubscriptionPass> findBySubscriptionId(Long subscriptionId);

    Optional<SubscriptionPass> findBySubscriptionIdAndRemainingCountGreaterThan(
            Long subscriptionId,
            int count
    );
}