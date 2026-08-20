package com.team1.cityfarm.service;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team1.cityfarm.entity.*;
import com.team1.cityfarm.global.exception.CustomError;
import com.team1.cityfarm.global.exception.CustomException;
import com.team1.cityfarm.portone.PortonePaymentClient;
import com.team1.cityfarm.portone.PortonePaymentResponseDto;
import com.team1.cityfarm.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
public class PortoneWebhookService {

    private final WebhookInboxRepository webhookInboxRepository;
    private final OrderRepository orderRepository;
    private final PortonePaymentClient portOnePaymentClient;
    private final PaymentService paymentService;
    // private final SubscriptionService subscriptionService; // 향후 3단계에서 추가 예정
    private final ObjectMapper objectMapper;

    @Value("${portone.webhook-secret}")
    private String webhookSecret;

    @Transactional
    public void processWebhook(String webhookId, String signature, String rawPayload) {
        // 1. [보안] 서명 검증 및 헤더 유무 확인
        if (webhookId == null || signature == null) {
            log.warn("[Webhook 에러] 헤더 누락");
            return;
        }
        verifySignature(rawPayload, signature);

        // 2. [멱등성] 이미 처리된 웹훅인지 검증
        if (webhookInboxRepository.existsByWebhookId(webhookId)) {
            log.info("[Webhook 중복 통지 방지] 이미 처리된 webhookId: {}", webhookId);
            return;
        }

        try {
            // 3. payload 파싱 후 paymentId 추출
            JsonNode rootNode = objectMapper.readTree(rawPayload);
            String paymentId = rootNode.path("data").path("paymentId").asText(null);

            if (paymentId == null) { // 포트원 V2 버전에 따라 JSON 필드명이 다를 수 있음
                paymentId = rootNode.path("payment_id").asText(null);
            }

            if (paymentId != null) {
                // 4. 결제 상태 처리 로직 위임
                handlePaymentPaid(paymentId);
            }

            // 5. 처리 완료 후 WebhookInbox 저장 (이후 동일 webhookId 진입 시 2번에서 방어)
            WebhookInbox inbox = WebhookInbox.builder()
                    .webhookId(webhookId)
                    .payload(rawPayload)
                    .status(WebhookStatus.PROCESSED)
                    .build();
            webhookInboxRepository.save(inbox);

        } catch (Exception e) {
            log.error("[Webhook 처리 중 예외 발생] webhookId: {}", webhookId, e);
            // 에러가 발생해도 웹훅 엔드포인트는 통상 200 OK를 내려주는 것이 좋습니다.
        }
    }

    /**
     * 포트원 실제 결제 내역을 조회하고 처리하는 핵심 로직
     */
    private void handlePaymentPaid(String paymentId) {
        // 1. 위조 방지를 위해 웹훅 내용이 아닌 PortOne API로 실제 상태를 단건 재조회
        PortonePaymentResponseDto portOnePayment = portOnePaymentClient.getPaymentDetails(paymentId);

        if (!"PAID".equalsIgnoreCase(portOnePayment.getStatus())) {
            log.info("[Webhook] 아직 PAID 상태가 아님. paymentId: {}, status: {}", paymentId, portOnePayment.getStatus());
            return;
        }

        // 2. 해당 결제건의 Order 조회 (가맹점 주문번호 추출 필요)
        // 주의: 웹훅 Payload에는 merchantOrderId가 있을 수도 없을 수도 있으므로 단건 조회를 통해 가져오거나 Order와 직접 매핑
        // 여기서는 단건 조회된 주문명이 있다고 가정 (결제 시 order_name 등에 세팅 시)
        // **포트원 V2는 paymentId로 Payment 조회 -> 연관 Order를 찾는 흐름이 필요할 수 있습니다.**
        // 아래는 Order에 merchantOrderId 가 잘 들어왔다고 가정
        String merchantOrderId = portOnePayment.getId(); // FIXME: PortOne 결제 객체에서 가맹점 주문번호(merchant_id)를 꺼내야 함

        Order order = orderRepository.findByMerchantOrderId(merchantOrderId)
                .orElse(null);

        if (order == null) {
            log.warn("[Webhook] 매핑되는 주문(Order)을 찾을 수 없음. merchantOrderId: {}", merchantOrderId);
            return;
        }

        // 3. [동시성 방어] 이미 브라우저(PaymentService.verifyAndCompletePayment)에서 PAID로 처리했다면 스킵
        if (order.getOrderStatus() == OrderStatus.PAID) {
            log.info("[Webhook] 이미 클라이언트 요청으로 처리된 결제건. merchantOrderId: {}", merchantOrderId);
            return;
        }

        // 4. 금액 검증 (위변조 방지)
        Integer actualPaidAmount = portOnePayment.getAmount().getPaid();
        if (actualPaidAmount == null || !actualPaidAmount.equals(order.getAmount())) {
            log.error("[Webhook 결제 위변조 위험] 결제 금액 불일치. Order: {}, PortOne: {}", order.getAmount(), actualPaidAmount);
            return;
        }

        // 5. 공통 승인 로직 호출 (PaymentService로 일원화된 로직)
        paymentService.processPaymentSuccess(
                order,
                paymentId,
                actualPaidAmount,
                portOnePayment.getPayMethodType(),
                portOnePayment.getApprovedAtParsed()
        );

        /*
         * 향후 [3단계]에서 구독 연장(SUBSCRIPTION) 결제도 처리하게 될 경우,
         * 여기서 order.getOrderType() == OrderType.SUBSCRIPTION 분기 처리 추가.
         */
    }

    /**
     * PortOne V2 HMAC-SHA256 서명 검증 로직
     */
    private void verifySignature(String payload, String signature) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);

            byte[] hashBytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String expectedSignature = Base64.getEncoder().encodeToString(hashBytes);

            // PortOne의 signature 형식에 따라 직접 비교하거나 파싱이 필요할 수 있습니다.
            if (!signature.contains(expectedSignature) && !signature.equals(expectedSignature)) {
                log.error("[Webhook 서명 불일치] 해킹/위조 의심 요청");
                throw new CustomException(CustomError.AUTH_UNAUTHORIZED);
            }
        } catch (Exception e) {
            log.error("[Webhook 서명 검증 실패]", e);
            throw new CustomException(CustomError.AUTH_UNAUTHORIZED);
        }
    }
}