package com.team1.cityfarm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PortOnePaymentService {

    // application.yml 에 설정해둔 포트원 API Secret Key
    @Value("${portone.api.secret}")
    private String portOneApiSecret;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 빌링키를 사용해 포트원 서버에 결제(승인) 요청
     */
    public void payWithBillingKey(String billingKey, String paymentId, int amount, String orderName, Long userId) {
        // 포트원 V2 빌링키 결제 API URL
        String url = "https://api.portone.io/payments/" + paymentId + "/billing-key";

        // 헤더 설정 (Authorization: PortOne <API_SECRET>)
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "PortOne " + portOneApiSecret);
        headers.set("Content-Type", "application/json");

        // 요청 바디 생성
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("billingKey", billingKey);
        requestBody.put("orderName", orderName);

        Map<String, Integer> amountMap = new HashMap<>();
        amountMap.put("total", amount);
        requestBody.put("amount", amountMap);

        requestBody.put("currency", "KRW");

        // 고객 정보 (필요시)
        Map<String, String> customerMap = new HashMap<>();
        customerMap.put("id", String.valueOf(userId));
        requestBody.put("customer", customerMap);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            // API 호출
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("포트원 결제 요청 실패: {}", response.getBody());
                throw new RuntimeException("결제 처리에 실패했습니다.");
            }
            log.info("포트원 결제 성공! PaymentId: {}", paymentId);

        } catch (Exception e) {
            log.error("포트원 통신 중 오류 발생", e);
            throw new RuntimeException("결제 서버와 통신 중 오류가 발생했습니다.");
        }
    }
}