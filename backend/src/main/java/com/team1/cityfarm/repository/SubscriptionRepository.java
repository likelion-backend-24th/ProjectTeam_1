package com.team1.cityfarm.repository;

import com.team1.cityfarm.entity.Subscription;
import com.team1.cityfarm.entity.SubscriptionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository
        extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findByUserId(Long userId);

    // 해지/환불 처리용 락 - 동시 요청(예: 수강권 사용과 환불 요청이 겹치는 경우) 직렬화
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Subscription s where s.id = :id")
    Optional<Subscription> findByIdForUpdate(@Param("id") Long id);

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

    // 구독 예약 복구 배치용 — 해지 예약되지 않았는데 SCHEDULED 상태 다음 회차 예약이
    // 하나도 없는 구독 조회 (1회차 결제 성공 뒤 PortOne 예약 실패로 방치된 케이스)
    @Query("""
            select s from Subscription s
            where s.status = :status
              and s.cancelAtPeriodEnd = false
              and not exists (
                  select 1 from SubscriptionSchedule sch
                  where sch.subscription = s
                    and sch.status = com.team1.cityfarm.entity.ScheduleStatus.SCHEDULED
              )
            """)
    List<Subscription> findActiveWithoutScheduledPayment(@Param("status") SubscriptionStatus status);
}
