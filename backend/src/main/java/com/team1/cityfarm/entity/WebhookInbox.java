package com.team1.cityfarm.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "webhook_inboxes",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_webhook_inbox_webhook_id",
                        columnNames = "webhook_id"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WebhookInbox {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * PortOne Webhook 이벤트 고유 ID
     */
    @Column(name = "webhook_id", nullable = false, unique = true)
    private String webhookId;

    /**
     * PortOne에서 전달한 이벤트 유형
     * 예: 결제 상태 변경 등
     */
    @Column(name = "event_type", nullable = false)
    private String eventType;

    /**
     * PortOne Payment ID
     * 결제 관련 Webhook이 아닌 경우 null 가능
     */
    @Column(name = "payment_id")
    private String paymentId;

    /**
     * Webhook 원문 데이터
     * JSON 형태로 저장
     */
    @Column(name = "payload", columnDefinition = "TEXT")
    private String payload;

    /**
     * Webhook 처리 상태
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WebhookStatus status;

    /**
     * Webhook을 서버가 수신한 시간
     */
    @Column(nullable = false)
    private LocalDateTime receivedAt;

    /**
     * 실제 Webhook 처리가 완료된 시간
     */
    private LocalDateTime processedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}