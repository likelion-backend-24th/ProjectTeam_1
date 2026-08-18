package com.team1.cityfarm.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity@Table(name = "subscription_schedule")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class SubscriptionSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    @Column(nullable = false)
    private Integer round;

    @Column(nullable = false)
    private LocalDateTime scheduledAt;

    @Column(nullable = false)
    private Integer amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ScheduleStatus status;

    /**
     * PortOne 예약 결제 식별자
     */
    @Column(name = "portone_schedule_id")
    private String portoneScheduleId;

    /**
     * 실제 결제가 발생한 이후 연결
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private Payment payment;

    @CreationTimestamp
    @Column(updatable = false, name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

