package com.team1.cityfarm.portone;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team1.cityfarm.global.exception.CustomError;
import com.team1.cityfarm.global.exception.CustomException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class PortonePaymentClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String webhookNoticeUrl;
    private final String storeId;

    // 생성자에서 공통 헤더(Authorization, Content-Type)를 미리 세팅하여 중복 제거
    public PortonePaymentClient(
            @Value("${portone.api-secret}") String apiSecret,
            @Value("${portone.api.base-url:https://api.portone.io}") String baseUrl,
            @Value("${portone.webhook-notice-url}") String webhookNoticeUrl,
            @Value("${portone.store-id}") String storeId,
            ObjectMapper objectMapper
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "PortOne " + apiSecret)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
        this.objectMapper = objectMapper;
        this.webhookNoticeUrl = webhookNoticeUrl;
        this.storeId = storeId;
    }

    // DELETE 요청 중 일부(/payment-schedules)는 실제 바디 대신 requestBody라는 이름의
    // 쿼리 파라미터에 JSON을 그대로 실어 보내야 한다(PortOne V2 OpenAPI 스펙 기준).
    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * [PortOne V2 API 단건 결제 내역 조회]
     */
    public PortonePaymentResponseDto getPaymentDetails(String paymentId) {
        try {
            return restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/payments/{paymentId}")
                            .queryParam("storeId", storeId)
                            .build(paymentId))
                    // defaultHeader로 인해 header("Authorization", ...) 생략 가능
                    .retrieve()
                    .body(PortonePaymentResponseDto.class);
        } catch (Exception e) {
            log.error("[PortOne API 조회 실패] paymentId: {}", paymentId, e);
            throw new CustomException(CustomError.PORTONE_API_ERROR);
        }
    }

    private static final int BILLING_KEY_LOOKUP_MAX_ATTEMPTS = 5;
    private static final long BILLING_KEY_LOOKUP_INITIAL_DELAY_MS = 500;
    // 재시도할 때마다 대기 시간을 늘려서(500ms, 1000ms, 1500ms, 2000ms) PortOne이 반영할
    // 시간을 더 넉넉히 준다 - 고정 간격이 너무 촉박하다는 리뷰 피드백 반영.
    private static final long BILLING_KEY_LOOKUP_BACKOFF_STEP_MS = 500;

    /**
     * [PortOne V2 API 빌링키 단건 조회]
     * 프론트에서 SDK 발급 응답을 받은 직후 바로 조회하면, PortOne 쪽에 빌링키가 아직
     * 반영되기 전이라 404(BILLING_KEY_NOT_FOUND)가 날 수 있다. 그 경우에만 재시도한다.
     */
    public PortoneBillingKeyResponseDto getBillingKeyDetails(String billingKey) {
        for (int attempt = 1; attempt <= BILLING_KEY_LOOKUP_MAX_ATTEMPTS; attempt++) {
            try {
                return restClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/billing-keys/{billingKey}")
                                .queryParam("storeId", storeId)
                                .build(billingKey))
                        .retrieve()
                        .body(PortoneBillingKeyResponseDto.class);
            } catch (HttpClientErrorException.NotFound e) {
                if (attempt == BILLING_KEY_LOOKUP_MAX_ATTEMPTS) {
                    log.error("[PortOne API 빌링키 조회 실패 - 재시도 소진] billingKey: {}", billingKey, e);
                    throw new CustomException(CustomError.PORTONE_API_ERROR);
                }
                long delay = BILLING_KEY_LOOKUP_INITIAL_DELAY_MS
                        + (attempt - 1) * BILLING_KEY_LOOKUP_BACKOFF_STEP_MS;
                log.warn("[PortOne API 빌링키 조회 재시도 {}/{}] billingKey: {} - 아직 반영되지 않음, {}ms 대기",
                        attempt, BILLING_KEY_LOOKUP_MAX_ATTEMPTS, billingKey, delay);
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new CustomException(CustomError.PORTONE_API_ERROR);
                }
            } catch (Exception e) {
                log.error("[PortOne API 빌링키 조회 실패] billingKey: {}", billingKey, e);
                throw new CustomException(CustomError.PORTONE_API_ERROR);
            }
        }
        throw new CustomException(CustomError.PORTONE_API_ERROR);
    }

    /**
     * [PortOne V2 API 결제 취소 요청 (전액 환불)]
     */
    public void cancelPayment(String paymentId, String reason) {
        try {
            restClient.post()
                    .uri("/payments/{paymentId}/cancel", paymentId)
                    .body(Map.of("storeId", storeId, "reason", reason))
                    .retrieve()
                    .toBodilessEntity();

            log.info("[PortOne API] 결제 취소 성공 - paymentId: {}", paymentId);
        } catch (Exception e) {
            log.error("[PortOne API] 결제 취소 실패 - paymentId: {}, error: {}", paymentId, e.getMessage());
            throw new CustomException(CustomError.PORTONE_CANCEL_FAILED);
        }
    }

    /**
     * [PortOne V2 API 빌링키 결제 요청 (구독 최초 결제/정기 결제 공용)]
     * 이 엔드포인트의 성공 응답(200)은 {"payment":{"pgTxId","paidAt"}}뿐이라 status/amount/
     * payMethod 등 상세 정보가 없다(portone-v2-openapi.json 기준). HTTP 2xx 자체가 승인
     * 성공을 의미하므로(실패 시 에러 응답이 옴), 승인 후 단건 조회 API로 상세 정보를 다시
     * 받아온다 — 위변조 방지를 위해 웹훅 내용을 안 믿고 재조회하는 다른 흐름과 동일한 관례.
     */
    public PortonePaymentResponseDto payWithBillingKey(String billingKey, String paymentId, String orderName, int amount, Long userId) {
        try {
            Map<String, Object> requestBody = Map.of(
                    "storeId", storeId,
                    "billingKey", billingKey,
                    "orderName", orderName,
                    "amount", Map.of("total", amount),
                    "currency", "KRW",
                    "customer", Map.of("id", String.valueOf(userId)),
                    // 스토어를 여러 팀이 공용으로 쓰고 있어 콘솔에 팀별 웹훅 URL을 등록할 수 없다.
                    // 결제 요청 시 noticeUrls로 직접 지정하면 콘솔 설정보다 우선 적용된다.
                    "noticeUrls", List.of(webhookNoticeUrl)
            );

            restClient.post()
                    .uri("/payments/{paymentId}/billing-key", paymentId)
                    .body(requestBody)
                    .retrieve()
                    .toBodilessEntity();

            PortonePaymentResponseDto response = getPaymentDetails(paymentId);

            if (response == null || !"PAID".equalsIgnoreCase(response.getStatus())) {
                log.error("[PortOne API] 빌링키 결제 승인 실패 - paymentId: {}, status: {}",
                        paymentId, response == null ? null : response.getStatus());
                throw new CustomException(CustomError.PORTONE_BILLING_FAILED);
            }

            log.info("[PortOne API] 빌링키 결제 성공 - paymentId: {}", paymentId);
            return response;
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("[PortOne API] 빌링키 결제 요청 실패 - paymentId: {}, error: {}", paymentId, e.getMessage());
            throw new CustomException(CustomError.PORTONE_BILLING_FAILED);
        }
    }

    /**
     * [PortOne V2 API 결제 예약 생성 (정기결제 다음 회차용)]
     * 회차마다 새 paymentId를 발급해 호출해야 한다.
     */
    public PortonePaymentScheduleResponseDto schedulePayment(
            String billingKey, String paymentId, String orderName, int amount, Long userId, LocalDateTime timeToPay
    ) {
        try {
            Map<String, Object> requestBody = Map.of(
                    "payment", Map.of(
                            "storeId", storeId,
                            "billingKey", billingKey,
                            "orderName", orderName,
                            "customer", Map.of("id", String.valueOf(userId)),
                            "amount", Map.of("total", amount),
                            "currency", "KRW",
                            // 예약결제는 PortOne이 나중에 자동 실행하므로, 그 결과를 통지받을 웹훅 주소가
                            // 이 필드 없이는 아예 없다(스토어 공용이라 콘솔 등록 불가 - payWithBillingKey와 동일한 이유).
                            "noticeUrls", List.of(webhookNoticeUrl)
                    ),
                    "timeToPay", timeToPay.atZone(ZoneId.of("Asia/Seoul")).toInstant().toString()
            );

            PortonePaymentScheduleResponseDto response = restClient.post()
                    .uri("/payments/{paymentId}/schedule", paymentId)
                    .body(requestBody)
                    .retrieve()
                    .body(PortonePaymentScheduleResponseDto.class);

            log.info("[PortOne API] 결제 예약 성공 - paymentId: {}, timeToPay: {}", paymentId, timeToPay);
            return response;
        } catch (Exception e) {
            log.error("[PortOne API] 결제 예약 실패 - paymentId: {}, error: {}", paymentId, e.getMessage());
            throw new CustomException(CustomError.PORTONE_BILLING_FAILED);
        }
    }

    /**
     * [PortOne V2 API 결제 예약 취소 (특정 예약 건 단위)]
     * 구독 해지 예약 시, 이미 잡혀 있는 다음 회차 예약 하나만 취소한다.
     */
    public void cancelSchedule(String portoneScheduleId) {
        try {
            // LinkedHashMap: Map.of()는 키 순서를 보장하지 않아 JSON 직렬화 결과가 매번 달라진다
            // (요청 자체는 문제없지만 회귀 테스트에서 정확한 문자열을 비교하므로 순서 고정 필요).
            Map<String, Object> body = new java.util.LinkedHashMap<>();
            body.put("storeId", storeId);
            body.put("scheduleIds", java.util.List.of(portoneScheduleId));
            String requestBody = toJson(body);
            restClient.method(org.springframework.http.HttpMethod.DELETE)
                    .uri(uriBuilder -> uriBuilder
                            .path("/payment-schedules")
                            .queryParam("requestBody", "{requestBody}")
                            .build(requestBody))
                    .retrieve()
                    .toBodilessEntity();

            log.info("[PortOne API] 결제 예약 취소 성공 - scheduleId: {}", portoneScheduleId);
        } catch (Exception e) {
            log.error("[PortOne API] 결제 예약 취소 실패 - scheduleId: {}, error: {}", portoneScheduleId, e.getMessage());
            throw new CustomException(CustomError.PORTONE_CANCEL_FAILED);
        }
    }

    /**
     * [PortOne V2 API 결제 예약 취소 (빌링키 기준 전체 취소)]
     * 구독 해지 시 해당 빌링키에 걸린 모든 미실행 예약을 취소한다.
     */
    public void cancelScheduleByBillingKey(String billingKey) {
        try {
            Map<String, Object> body = new java.util.LinkedHashMap<>();
            body.put("storeId", storeId);
            body.put("billingKey", billingKey);
            String requestBody = toJson(body);
            restClient.method(org.springframework.http.HttpMethod.DELETE)
                    .uri(uriBuilder -> uriBuilder
                            .path("/payment-schedules")
                            .queryParam("requestBody", "{requestBody}")
                            .build(requestBody))
                    .retrieve()
                    .toBodilessEntity();

            log.info("[PortOne API] 결제 예약 취소 성공 - billingKey 기준");
        } catch (Exception e) {
            log.error("[PortOne API] 결제 예약 취소 실패 - error: {}", e.getMessage());
            throw new CustomException(CustomError.PORTONE_CANCEL_FAILED);
        }
    }

    /**
     * [PortOne V2 API 빌링키 해지]
     * 카드 변경 시, 마이그레이션이 끝나 더 이상 아무 것도 참조하지 않는 이전 빌링키를 PortOne에서 제거한다.
     * reason은 body가 아니라 query parameter로 전달해야 한다(PortOne V2 OpenAPI 스펙 기준).
     * 이미 예약결제가 걸려있는 빌링키를 지우려 하면 409(PaymentScheduleAlreadyExistsError)가 나므로,
     * 호출 전에 반드시 해당 빌링키의 예약을 먼저 취소/마이그레이션해야 한다.
     */
    public void deleteBillingKey(String billingKey, String reason) {
        try {
            restClient.method(org.springframework.http.HttpMethod.DELETE)
                    .uri(uriBuilder -> uriBuilder
                            .path("/billing-keys/{billingKey}")
                            .queryParam("reason", "{reason}")
                            .queryParam("storeId", "{storeId}")
                            .build(billingKey, reason, storeId))
                    .retrieve()
                    .toBodilessEntity();

            log.info("[PortOne API] 빌링키 해지 성공 - billingKey: {}", billingKey);
        } catch (Exception e) {
            log.error("[PortOne API] 빌링키 해지 실패 - billingKey: {}, error: {}", billingKey, e.getMessage());
            // 이 실패는 트랜잭션을 막지 않도록 호출부(BillingKeyService)에서 예외를 삼킬 예정
            throw new CustomException(CustomError.PORTONE_CANCEL_FAILED);
        }
    }
}