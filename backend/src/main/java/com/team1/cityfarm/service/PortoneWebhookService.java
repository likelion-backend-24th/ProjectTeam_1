package com.team1.cityfarm.service;


import com.team1.cityfarm.entity.*;
import com.team1.cityfarm.portone.PortoneWebhookDto;
import com.team1.cityfarm.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class PortoneWebhookService {

    private final PaymentRepository paymentRepository;
    private final SubscriptionPassRepository subscriptionPassRepository;

    /**
     * [Webhook 이벤트 분기 처리]
     */
    @Transactional
    public void processWebhook(PortoneWebhookDto webhookDto) {
        if (webhookDto == null || webhookDto.getData() == null) {
            log.warn("[Webhook Ignored] 유효하지 않은 Webhook 데이터입니다.");
            return;
        }

        String eventType = webhookDto.getType();
        String paymentId = webhookDto.getData().getPaymentId();

        switch (eventType) {
            case "Transaction.Paid":
            case "PAYMENT_PAID":
                handlePaymentPaid(paymentId);
                break;

            case "Transaction.Failed":
            case "PAYMENT_FAILED":
                handlePaymentFailed(paymentId);
                break;

            case "Transaction.Cancelled":
            case "PAYMENT_CANCELLED":
                handlePaymentCancelled(paymentId);
                break;

            default:
                log.info("[Webhook Unhandled Event] 처리하지 않는 이벤트 타입입니다: {}", eventType);
                break;
        }
    }

    /**
     * [결제 성공 처리]
     * Payment(approvedAt, status) 및 Order -> Subscription(ACTIVE 연장), 수강권 갱신
     */
    private void handlePaymentPaid(String paymentId) {
        Payment payment = paymentRepository.findByPortonePaymentId(paymentId)
                .orElseGet(() -> {
                    log.warn("[Webhook Warning] 존재하지 않는 결제건 수신: {}", paymentId);
                    return null;
                });

        if (payment == null) return;

        // 멱등성 보장 (이미 PAID 처리된 건 중복 처리 방지)
        if (payment.getStatus() == PaymentStatus.PAID) {
            log.info("[Webhook Redundant] 이미 완료된 결제건입니다: {}", paymentId);
            return;
        }

        // 1. 결제 완료 상태 및 승인 시각(approvedAt) 업데이트
        payment.setStatus(PaymentStatus.PAID);
        payment.setApprovedAt(LocalDateTime.now());

        // 2. Payment -> Order -> Subscription 연관 관계 참조 및 구독 연장
        Order order = payment.getOrder();
        if (order != null && order.getSubscription() != null) {
            Subscription subscription = order.getSubscription();
            subscription.setStatus(SubscriptionStatus.ACTIVE);

            // 다음 구독 주기 계산 (기존 만료일로부터 1개월 연장)
            LocalDateTime newPeriodStart = subscription.getCurrentPeriodEnd();
            LocalDateTime newPeriodEnd = newPeriodStart.plusMonths(1);

            subscription.setCurrentPeriodStart(newPeriodStart);
            subscription.setCurrentPeriodEnd(newPeriodEnd);

            // 단건 수강권 갱신
            renewSubscriptionPass(subscription);

            log.info("[Webhook 구독 연장 완료] subscriptionId: {}, newValidUntil: {}",
                    subscription.getId(), newPeriodEnd);
        }

        log.info("[Webhook 결제 성공 동기화 완료] paymentId: {}", paymentId);
    }

    /**
     * [결제 실패 처리]
     * 구독 상태를 EXPIRED로 변경 처리
     */
    private void handlePaymentFailed(String paymentId) {
        Payment payment = paymentRepository.findByPortonePaymentId(paymentId)
                .orElse(null);

        if (payment == null) return;

        payment.setStatus(PaymentStatus.FAILED);

        Order order = payment.getOrder();
        if (order != null && order.getSubscription() != null) {
            Subscription subscription = order.getSubscription();
            subscription.setStatus(SubscriptionStatus.EXPIRED);
            log.warn("[Webhook 구독 결제 실패] subscriptionId: {}. 구독 상태를 EXPIRED로 변경합니다.", subscription.getId());
        }
    }

    /**
     * [결제 취소 처리]
     * Payment 엔티티의 cancel() 메서드 활용
     */
    private void handlePaymentCancelled(String paymentId) {
        Payment payment = paymentRepository.findByPortonePaymentId(paymentId)
                .orElse(null);

        if (payment == null) return;

        payment.cancel("Portone Webhook 수신에 의한 취소", LocalDateTime.now());
        log.info("[Webhook 결제 취소 동기화 완료] paymentId: {}", paymentId);
    }

    /**
     * [수강권 단건 갱신]
     */
    private void renewSubscriptionPass(Subscription subscription) {
        subscriptionPassRepository.findBySubscriptionId(subscription.getId())
                .ifPresent(pass -> pass.setStatus(PassStatus.EXPIRED));

        SubscriptionPass newPass = SubscriptionPass.builder()
                .subscription(subscription)
                .totalCount(2)
                .remainingCount(2)
                .validFrom(subscription.getCurrentPeriodStart())
                .validUntil(subscription.getCurrentPeriodEnd())
                .status(PassStatus.ACTIVE)
                .build();

        subscriptionPassRepository.save(newPass);
    }
}