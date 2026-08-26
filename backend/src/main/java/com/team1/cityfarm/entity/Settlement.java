package com.team1.cityfarm.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter@Table(name = "settlements")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Settlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 어떤 주문으로 발생한 정산인지
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    /**
     * 구독 수강권으로 신청한 건에 대한 정산인 경우, 어떤 신청(ClassEnrollment)에서 발생했는지.
     * 이 경로는 Order가 없어(order=null) order_id로 역추적이 불가능하므로 별도로 보관한다.
     */
    @Column(name = "enrollment_id")
    private Long enrollmentId;

    /**
     * 정산을 받을 HOST
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id", nullable = false)
    private User host;


    /**
     * 정산된 클래스 이름
     */
    @Column(name = "class_name")
    private String className;

    /**
     * 정산 대상 콘텐츠 유형
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SettlementType settlementType;

    /**
     * USER가 실제 결제한 금액
     */
    @Column(nullable = false)
    private int paymentAmount;

    /**
     * 결제 당시 적용된 정산 비율
     * 현재 CLASS / FIELD_RENTAL 모두 50%
     */
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal settlementRate;

    /**
     * HOST에게 정산될 금액
     */
    @Column(nullable = false)
    private int settlementAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SettlementStatus status;

    /**
     * 정산 완료 시각
     */
    private LocalDateTime settledAt;

    @CreationTimestamp
    @Column(updatable = false, name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder
    public Settlement(
            Order order,
            Long enrollmentId,
            User host,
            String className,
            SettlementType settlementType,
            int paymentAmount,
            BigDecimal settlementRate,
            int settlementAmount
    ) {
        this.order = order;
        this.enrollmentId = enrollmentId;
        this.host = host;
        this.className = className;
        this.settlementType = settlementType;
        this.paymentAmount = paymentAmount;
        this.settlementRate = settlementRate;
        this.settlementAmount = settlementAmount;
        this.status = SettlementStatus.PENDING;
    }

    public void complete() {
        this.status = SettlementStatus.COMPLETED;
        this.settledAt = LocalDateTime.now();
    }

    public void cancel() {
        this.status = SettlementStatus.CANCELLED;
    }
}