package com.team1.cityfarm.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "billing_key_issuance_intents",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_billing_key_issuance_intent_issue_id",
                        columnNames = "issue_id"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BillingKeyIssuanceIntent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 빌링키 발급을 요청한 사용자
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * 빌링키 발급 요청 식별자
     */
    @Column(name = "issue_id", nullable = false, unique = true)
    private String issueId;

    /**
     * 빌링키 발급 요청 상태
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BillingKeyIssuanceStatus status;

    /**
     * 발급 요청 만료 시간
     */
    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}