package com.team1.cityfarm.repository;

import com.team1.cityfarm.entity.PassStatus;
import com.team1.cityfarm.entity.SubscriptionPass;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SubscriptionPassRepository
        extends JpaRepository<SubscriptionPass, Long> {

    // 갱신마다 새 Pass row가 누적되므로 subscriptionId 단독 조회는 2회차 이상부터
    // 여러 건이 매칭되어 예외가 난다. 항상 status를 함께 지정해서 조회할 것.
    Optional<SubscriptionPass> findBySubscriptionIdAndStatus(Long subscriptionId, PassStatus status);

    // 환불 처리용 락 - 환불 자격 판단(미사용 여부) 이후 다른 트랜잭션이 이 수강권을
    // 소진하지 못하도록 잠근다(enrollClassWithPass의 UPDATE가 커밋 시점까지 블로킹됨).
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from SubscriptionPass p where p.subscription.id = :subscriptionId and p.status = :status")
    Optional<SubscriptionPass> findBySubscriptionIdAndStatusForUpdate(
            @Param("subscriptionId") Long subscriptionId,
            @Param("status") PassStatus status
    );

    Optional<SubscriptionPass> findBySubscriptionIdAndRemainingCountGreaterThan(
            Long subscriptionId,
            int count
    );

    Optional<SubscriptionPass> findBySubscriptionIdAndStatusAndRemainingCountGreaterThan(
            Long subscriptionId,
            PassStatus status,
            int count
    );
}