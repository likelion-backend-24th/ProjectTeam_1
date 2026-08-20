package com.team1.cityfarm.repository;

import com.team1.cityfarm.entity.PassStatus;
import com.team1.cityfarm.entity.SubscriptionPass;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubscriptionPassRepository
        extends JpaRepository<SubscriptionPass, Long> {

    // 갱신마다 새 Pass row가 누적되므로 subscriptionId 단독 조회는 2회차 이상부터
    // 여러 건이 매칭되어 예외가 난다. 항상 status를 함께 지정해서 조회할 것.
    Optional<SubscriptionPass> findBySubscriptionIdAndStatus(Long subscriptionId, PassStatus status);

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