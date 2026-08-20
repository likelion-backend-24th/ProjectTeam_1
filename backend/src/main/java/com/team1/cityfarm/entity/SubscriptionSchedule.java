package com.team1.cityfarm.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity@Table(name = "subscription_schedule")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class SubscriptionSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 어떤 구독의 정기결제인지
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    /**
     * 구독 결제 회차
     * 예: 1회차, 2회차, 3회차...
     */
    @Column(nullable = false)
    private Integer round;

    /**
     * PortOne에 예약한 결제 예정 시각
     */
    @Column(nullable = false,name = "scheduled_at")
    private LocalDateTime scheduledAt;

    /**
     * 해당 회차에 결제할 구독 금액
     */
    @Column(nullable = false)
    private Integer amount;

    /**
     * 예약 결제 상태
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ScheduleStatus status;

    /**
     * PortOne 예약 결제 식별자
     */
    @Column(name = "portone_schedule_id")
    private String portoneScheduleId;

    /**
     * 이 회차 결제 요청 시 발급한 paymentId
     * 결제 실행 후 도착하는 웹훅(paymentId만 포함)으로 이 회차를 역추적하는 데 사용한다.
     */
    @Column(name = "payment_id")
    private String paymentId;

    /**
     * 실제 결제가 실행된 이후 생성된 주문
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @CreationTimestamp
    @Column(updatable = false, name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

