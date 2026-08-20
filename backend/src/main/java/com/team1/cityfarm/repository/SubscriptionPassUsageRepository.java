package com.team1.cityfarm.repository;

import com.team1.cityfarm.entity.SubscriptionPassUsage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubscriptionPassUsageRepository
        extends JpaRepository<SubscriptionPassUsage, Long> {

    List<SubscriptionPassUsage> findBySubscriptionPassIdOrderByCreatedAtDesc(
            Long subscriptionPassId
    );

    Optional<SubscriptionPassUsage> findByEnrollmentId(
            Long enrollmentId
    );

    boolean existsByEnrollmentId(Long enrollmentId);
}