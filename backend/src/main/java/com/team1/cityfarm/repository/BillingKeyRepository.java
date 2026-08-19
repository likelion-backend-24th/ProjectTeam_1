package com.team1.cityfarm.repository;

import com.team1.cityfarm.entity.BillingKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BillingKeyRepository
        extends JpaRepository<BillingKey, Long> {

    Optional<BillingKey> findByUserId(Long userId);

    Optional<BillingKey> findByBillingKeyEncrypted(String billingKeyEncrypted);

    boolean existsByUserId(Long userId);
}