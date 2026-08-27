package com.team1.cityfarm.portone;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PortonePaymentResponseDto {

    private String id;              // PortOne 결제 고유 ID (paymentId)
    private String status;          // PAID, CANCELLED, FAILED 등
    private Amount amount;          // 결제 금액 정보
    private String orderName;       // 주문명
    private String paidAt;          // 결제 완료 일시 (ISO-8601 UTC 문자열)
    private Method method;          // 결제 수단 상세 (PortOne V2 API)

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Amount {
        private Integer total;      // 총 결제 요청 금액
        private Integer paid;       // 실제 결제된 금액
        private Integer cancelled;  // 취소된 금액
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Method {
        private String type;        // Card, EasyPay, Transfer 등
        private String provider;    // KAKAO_PAY, CARD 등
    }

    /**
     * ISO-8601 형식의 paidAt 문자열을 LocalDateTime(한국 시간)으로 변환
     */
    public LocalDateTime getApprovedAtParsed() {
        if (this.paidAt == null) return null;
        return Instant.parse(this.paidAt)
                .atZone(ZoneId.of("Asia/Seoul"))
                .toLocalDateTime();
    }

    /**
     * 결제 수단 유형 추출
     */
    public String getPayMethodType() {
        if (this.method == null) return "UNKNOWN";
        return this.method.getProvider() != null ? this.method.getProvider() : this.method.getType();
    }
}