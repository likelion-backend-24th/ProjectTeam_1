package com.team1.cityfarm.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "subscription_pass_usages",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_subscription_pass_usage_enrollment",
                        columnNames = "enrollment_id"
                )
        }
)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SubscriptionPassUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 사용한 구독 수강권
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_pass_id", nullable = false)
    private SubscriptionPass subscriptionPass;

    /**
     * 수강권을 사용한 클래스 신청
     * ClassEnrollment 추가시 주석 제거
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enrollment_id", nullable = false)
    private ClassEnrollment enrollment;

    /**
     * 수강권 차감 시각
     */
    @Column(nullable = false)
    private LocalDateTime usedAt;

    @CreationTimestamp
    @Column(updatable = false, name = "created_at")
    private LocalDateTime createdAt;
}