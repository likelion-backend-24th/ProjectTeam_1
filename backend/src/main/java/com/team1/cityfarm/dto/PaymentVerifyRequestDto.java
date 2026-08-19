package com.team1.cityfarm.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PaymentVerifyRequestDto {

    private String paymentId;        // PortOne에서 발급된 결제 고유 ID
    private String merchantOrderId;  // 백엔드에서 생성했던 주문 번호
}