package com.team1.cityfarm.portone;

import com.team1.cityfarm.global.exception.CustomError;
import com.team1.cityfarm.global.exception.CustomException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

@Slf4j
@Component
public class PortonePaymentClient {

    private final RestClient restClient;

    // 생성자에서 공통 헤더(Authorization, Content-Type)를 미리 세팅하여 중복 제거
    public PortonePaymentClient(
            @Value("${portone.api-secret}") String apiSecret,
            @Value("${portone.api.base-url:https://api.portone.io}") String baseUrl
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "PortOne " + apiSecret)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * [PortOne V2 API 단건 결제 내역 조회]
     */
    public PortonePaymentResponseDto getPaymentDetails(String paymentId) {
        try {
            return restClient.get()
                    .uri("/payments/{paymentId}", paymentId)
                    // defaultHeader로 인해 header("Authorization", ...) 생략 가능
                    .retrieve()
                    .body(PortonePaymentResponseDto.class);
        } catch (Exception e) {
            log.error("[PortOne API 조회 실패] paymentId: {}", paymentId, e);
            throw new CustomException(CustomError.PORTONE_API_ERROR);
        }
    }

    /**
     * [PortOne V2 API 빌링키 단건 조회]
     */
    public PortoneBillingKeyResponseDto getBillingKeyDetails(String billingKey) {
        try {
            return restClient.get()
                    .uri("/billing-keys/{billingKey}", billingKey)
                    .retrieve()
                    .body(PortoneBillingKeyResponseDto.class);
        } catch (Exception e) {
            log.error("[PortOne API 빌링키 조회 실패] billingKey: {}", billingKey, e);
            throw new CustomException(CustomError.PORTONE_API_ERROR);
        }
    }

    /**
     * [PortOne V2 API 결제 취소 요청 (전액 환불)]
     */
    public void cancelPayment(String paymentId, String reason) {
        try {
            restClient.post()
                    .uri("/payments/{paymentId}/cancel", paymentId)
                    .body(Map.of("reason", reason))
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
     * HTTP 2xx 여부만으로 성공을 판단하지 않고, 응답 바디의 결제 상태(status)까지 PAID인지 확인한다.
     */
    public PortonePaymentResponseDto payWithBillingKey(String billingKey, String paymentId, String orderName, int amount, Long userId) {
        try {
            Map<String, Object> requestBody = Map.of(
                    "billingKey", billingKey,
                    "orderName", orderName,
                    "amount", Map.of("total", amount),
                    "currency", "KRW",
                    "customer", Map.of("id", String.valueOf(userId))
            );

            PortonePaymentResponseDto response = restClient.post()
                    .uri("/payments/{paymentId}/billing-key", paymentId)
                    .body(requestBody)
                    .retrieve()
                    .body(PortonePaymentResponseDto.class);

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
                            "billingKey", billingKey,
                            "orderName", orderName,
                            "customer", Map.of("id", String.valueOf(userId)),
                            "amount", Map.of("total", amount),
                            "currency", "KRW"
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
     * [PortOne V2 API 결제 예약 취소 (빌링키 기준 전체 취소)]
     * 구독 해지 시 해당 빌링키에 걸린 모든 미실행 예약을 취소한다.
     */
    public void cancelScheduleByBillingKey(String billingKey) {
        try {
            restClient.method(org.springframework.http.HttpMethod.DELETE)
                    .uri("/payment-schedules")
                    .body(Map.of("billingKey", billingKey))
                    .retrieve()
                    .toBodilessEntity();

            log.info("[PortOne API] 결제 예약 취소 성공 - billingKey 기준");
        } catch (Exception e) {
            log.error("[PortOne API] 결제 예약 취소 실패 - error: {}", e.getMessage());
            throw new CustomException(CustomError.PORTONE_CANCEL_FAILED);
        }
    }
}