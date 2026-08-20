package com.team1.cityfarm.repository;

import com.team1.cityfarm.entity.BillingKey;
import com.team1.cityfarm.entity.BillingKeyStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BillingKeyRepository
        extends JpaRepository<BillingKey, Long> {

    // REVOKED row가 계속 쌓이므로(소프트 삭제) 단건 조회는 반드시 status를 함께 지정해야 한다
    // (findByUserId 단독 조회는 유저당 row가 2개 이상이면 예외가 나서 일부러 없앰).
    Optional<BillingKey> findByUserIdAndStatus(Long userId, BillingKeyStatus status);

    Optional<BillingKey> findByBillingKeyEncrypted(String billingKeyEncrypted);

    boolean existsByUserId(Long userId);
}