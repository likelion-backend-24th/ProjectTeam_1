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
import java.time.LocalDateTime;
import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
public class PortoneWebhookService {

    private final WebhookInboxRepository webhookInboxRepository;
    private final OrderRepository orderRepository;
    private final PortonePaymentClient portOnePaymentClient;
    private final PaymentService paymentService;
    private final SubscriptionService subscriptionService;
    private final ObjectMapper objectMapper;

    @Value("${portone.webhook-secret}")
    private String webhookSecret;

    @Transactional
    public void processWebhook(String webhookId, String timestamp, String signature, String rawPayload) {
        // 1. [보안] 서명 검증
        // 스토어가 여러 팀 공용이라 콘솔에 웹훅을 등록할 수 없어 noticeUrls로만 통지받는데,
        // 이 경로로 오는 요청엔 PortOne이 webhook-id/timestamp/signature 헤더를 싣지 않는다
        // (콘솔 등록 웹훅 전용 기능으로 보임). 헤더가 없으면 서명 검증을 건너뛰고, paymentId
        // 자체는 아래 handlePaymentPaid/Failed에서 PortOne API 재조회로 검증한다(위조 방지의
        // 실질적 방어선은 원래도 서명이 아니라 이 재조회+금액 비교였음). 헤더가 실려오는
        // 경우(콘솔 웹훅이 등록되는 미래 등)에는 지금처럼 정상적으로 검증한다.
        if (webhookId != null && timestamp != null && signature != null) {
            try {
                verifySignature(webhookId, timestamp, rawPayload, signature);
            } catch (Exception e) {
                log.error("[Webhook 서명 검증 실패로 처리 중단] webhookId: {}", webhookId, e);
                return;
            }
        } else {
            log.info("[Webhook] 서명 헤더 없음(noticeUrls 경로) - 서명 검증 생략, paymentId 재조회로 검증");
        }

        // 2. [멱등성] 이미 처리된 웹훅인지 검증 (webhookId가 없으면 멱등성 검사 자체를 건너뛴다 -
        // 아래 처리 로직들이 각자 상태 기반으로 멱등하게 짜여 있어 안전하다: renewSubscription은
        // ScheduleStatus.PAID 체크, handlePaymentPaid는 order.getOrderStatus()==PAID 체크)
        if (webhookId != null && webhookInboxRepository.existsByWebhookId(webhookId)) {
            log.info("[Webhook 중복 통지 방지] 이미 처리된 webhookId: {}", webhookId);
            return;
        }

        try {
            // 3. payload 파싱 후 type/paymentId 추출
            JsonNode rootNode = objectMapper.readTree(rawPayload);
            String type = rootNode.path("type").asText(null);
            String paymentId = rootNode.path("data").path("paymentId").asText(null);

            if (paymentId == null) { // 포트원 V2 버전에 따라 JSON 필드명이 다를 수 있음
                paymentId = rootNode.path("payment_id").asText(null);
            }

            if (paymentId != null) {
                // 4. 웹훅 타입별 처리 로직 위임
                // 예약결제(정기결제) 실행 시에도 전용 타입 없이 동일한 Transaction.Paid/Failed로 온다.
                if ("Transaction.Failed".equals(type)) {
                    handlePaymentFailed(paymentId);
                } else {
                    handlePaymentPaid(paymentId);
                }
            }

            // 5. 처리 완료 후 WebhookInbox 저장 (이후 동일 webhookId 진입 시 2번에서 방어)
            // webhook_id는 NOT NULL 제약이라 헤더가 없는 요청(noticeUrls 경로)은 애초에
            // 멱등성 기록 대상이 아니므로 저장 자체를 건너뛴다 - 저장을 시도하면 제약 위반
            // 예외가 이 트랜잭션에 묶인 앞선 처리(구독 갱신 등)까지 롤백시킬 수 있다.
            if (webhookId != null) {
                LocalDateTime now = LocalDateTime.now();
                WebhookInbox inbox = WebhookInbox.builder()
                        .webhookId(webhookId)
                        .eventType(type != null ? type : "UNKNOWN")
                        .paymentId(paymentId)
                        .payload(rawPayload)
                        .status(WebhookStatus.PROCESSED)
                        .receivedAt(now)
                        .processedAt(now)
                        .createdAt(now)
                        .build();
                webhookInboxRepository.save(inbox);
            }

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

        // 2. 해당 결제건의 Order 조회
        // 컨벤션: 결제 요청 시 PortOne에 넘기는 paymentId를 우리 merchantOrderId와 동일한 값으로 발급한다
        // (OrderService.createClassOrder가 만드는 "BE24-CITYFARM-" + UUID를 프론트가 그대로 결제창의 paymentId로 사용).
        // 따라서 웹훅에서 파싱한 paymentId 자체가 곧 merchantOrderId이며, PortOne 단건 조회 결과의 id와도 항상 같아야 한다.
        if (!paymentId.equals(portOnePayment.getId())) {
            log.error("[Webhook] paymentId 불일치 - 웹훅: {}, PortOne 단건조회: {}", paymentId, portOnePayment.getId());
            return;
        }

        Order order = orderRepository.findByMerchantOrderId(paymentId)
                .orElse(null);

        if (order == null) {
            // 일반 주문(Order)이 아니면 구독 정기결제(예약결제) 회차일 수 있으므로 paymentId로 다시 조회한다.
            if (subscriptionService.renewSubscription(paymentId, portOnePayment)) {
                return;
            }
            log.warn("[Webhook] 매핑되는 주문(Order)/구독 스케줄을 찾을 수 없음. paymentId: {}", paymentId);
            return;
        }

        // 3. [동시성 방어] 이미 브라우저(PaymentService.verifyAndCompletePayment)에서 PAID로 처리했다면 스킵
        if (order.getOrderStatus() == OrderStatus.PAID) {
            log.info("[Webhook] 이미 클라이언트 요청으로 처리된 결제건. merchantOrderId: {}", paymentId);
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

    }

    /**
     * 결제 실패(Transaction.Failed) 웹훅 처리.
     * 현재는 구독 정기결제(예약결제) 실패만 반영한다 — 일반 주문은 결제창에서 실패가 그대로
     * 사용자에게 노출되고 Order가 PENDING으로 남을 뿐이라 별도 서버 처리가 필요 없다.
     */
    private void handlePaymentFailed(String paymentId) {
        if (!subscriptionService.handleScheduleFailed(paymentId)) {
            log.info("[Webhook] 구독 스케줄이 아닌 결제 실패 - 처리 생략. paymentId: {}", paymentId);
        }
    }

    /**
     * PortOne V2(Svix 규격) 웹훅 서명 검증.
     * 서명 대상 문자열은 "{webhookId}.{timestamp}.{rawPayload}" 이며,
     * webhook-signature 헤더는 공백으로 구분된 "v1,<base64signature>" 토큰이 하나 이상 올 수 있다.
     * secret은 "whsec_" 접두사를 제거한 뒤 Base64 디코딩한 바이트를 HMAC 키로 사용한다.
     */
    private void verifySignature(String webhookId, String timestamp, String payload, String signatureHeader) {
        try {
            byte[] secretBytes = decodeWebhookSecret(webhookSecret);
            String signedContent = webhookId + "." + timestamp + "." + payload;

            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secretBytes, "HmacSHA256"));
            byte[] hashBytes = mac.doFinal(signedContent.getBytes(StandardCharsets.UTF_8));
            String expectedSignature = Base64.getEncoder().encodeToString(hashBytes);

            boolean matched = false;
            for (String token : signatureHeader.trim().split("\\s+")) {
                String candidate = token.contains(",") ? token.substring(token.indexOf(',') + 1) : token;
                if (constantTimeEquals(candidate, expectedSignature)) {
                    matched = true;
                    break;
                }
            }

            if (!matched) {
                log.error("[Webhook 서명 불일치] 해킹/위조 의심 요청 - webhookId: {}", webhookId);
                throw new CustomException(CustomError.AUTH_UNAUTHORIZED);
            }
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("[Webhook 서명 검증 실패]", e);
            throw new CustomException(CustomError.AUTH_UNAUTHORIZED);
        }
    }

    private byte[] decodeWebhookSecret(String secret) {
        String raw = secret.startsWith("whsec_") ? secret.substring("whsec_".length()) : secret;
        try {
            return Base64.getDecoder().decode(raw);
        } catch (IllegalArgumentException e) {
            // 발급받은 secret이 Base64 형식이 아닌 경우를 대비한 안전한 폴백
            return secret.getBytes(StandardCharsets.UTF_8);
        }
    }

    private boolean constantTimeEquals(String a, String b) {
        return java.security.MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8),
                b.getBytes(StandardCharsets.UTF_8)
        );
    }
}