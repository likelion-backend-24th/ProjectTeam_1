package com.team1.cityfarm.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity@Table(name = "payments")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter@Setter
public class Payment {
    @Id@GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    /**
     * PortOne의 paymentId
     */
    @Column(nullable = false, unique = true)
    private String portonePaymentId;

    private String payMethod;

    @Column(nullable = false)
    private Integer amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    private LocalDateTime approvedAt;

    private LocalDateTime cancelledAt;

    private String cancelReason;

    @CreationTimestamp
    @Column(updatable = false, name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public void cancel(String cancelReason, LocalDateTime cancelledAt) {
        this.status = PaymentStatus.CANCELLED;
        this.cancelReason = cancelReason;
        this.cancelledAt = cancelledAt;
    }
}
