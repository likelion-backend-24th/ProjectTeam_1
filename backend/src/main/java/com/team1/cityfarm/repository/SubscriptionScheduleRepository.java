package com.team1.cityfarm.repository;

import com.team1.cityfarm.entity.ScheduleStatus;
import com.team1.cityfarm.entity.SubscriptionSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubscriptionScheduleRepository
        extends JpaRepository<SubscriptionSchedule, Long> {

    Optional<SubscriptionSchedule> findBySubscriptionId(
            Long subscriptionId
    );

    List<SubscriptionSchedule> findBySubscriptionIdOrderByScheduledAtDesc(
            Long subscriptionId
    );

    List<SubscriptionSchedule> findByStatus(
            ScheduleStatus status
    );

    Optional<SubscriptionSchedule> findByPaymentId(
            String paymentId
    );

    Optional<SubscriptionSchedule> findBySubscriptionIdAndStatus(
            Long subscriptionId,
            ScheduleStatus status
    );
}