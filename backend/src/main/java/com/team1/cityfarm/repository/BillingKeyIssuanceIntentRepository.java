package com.team1.cityfarm.repository;

import com.team1.cityfarm.entity.BillingKeyIssuanceIntent;
import com.team1.cityfarm.entity.BillingKeyIssuanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BillingKeyIssuanceIntentRepository
        extends JpaRepository<BillingKeyIssuanceIntent, Long> {

    Optional<BillingKeyIssuanceIntent> findByUserId(Long userId);

    Optional<BillingKeyIssuanceIntent> findByUserIdAndStatus(
            Long userId,
            BillingKeyIssuanceStatus status
    );
}