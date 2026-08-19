package com.team1.cityfarm.portone;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PortonePaymentResponseDto {

    private String id;              // PortOne 결제 고유 ID (paymentId)
    private String status;          // PAID, CANCELLED, FAILED, VIRTUAL_ACCOUNT_ISSUED 등
    private Amount amount;          // 결제 금액 정보
    private String orderName;       // 주문명
    private String paidAt;          // 결제 완료 일시 (ISO-8601 UTC)

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Amount {
        private Integer total;      // 총 결제 요청 금액
        private Integer paid;       // 실제 결제된 금액
        private Integer cancelled;  // 취소된 금액
    }
}