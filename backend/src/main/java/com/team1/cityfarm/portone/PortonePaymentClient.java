package com.team1.cityfarm.portone;

import com.team1.cityfarm.global.exception.CustomError;
import com.team1.cityfarm.global.exception.CustomException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Slf4j
@Component
public class PortonePaymentClient {

    private final RestClient restClient;

    // 생성자에서 공통 헤더(Authorization, Content-Type)를 미리 세팅하여 중복 제거
    public PortonePaymentClient(
            @Value("${portone.api.secret-key}") String apiSecret,
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
     * [PortOne V2 API 빌링키 정기 결제 요청 (스케줄러/정기구독용)]
     */
    public void executeBillingPayment(String billingKey, String paymentId, String orderName, int amount) {
        try {
            Map<String, Object> requestBody = Map.of(
                    "billingKey", billingKey,
                    "orderName", orderName,
                    "amount", Map.of("total", amount),
                    "currency", "KRW"
            );

            restClient.post()
                    .uri("/payments/{paymentId}/billing-key", paymentId)
                    .body(requestBody)
                    .retrieve()
                    .toBodilessEntity();

            log.info("[PortOne API] 빌링키 정기 결제 요청 성공 - paymentId: {}", paymentId);
        } catch (Exception e) {
            log.error("[PortOne API] 빌링키 정기 결제 요청 실패 - paymentId: {}, error: {}", paymentId, e.getMessage());
            throw new CustomException(CustomError.PORTONE_BILLING_FAILED);
        }
    }
}