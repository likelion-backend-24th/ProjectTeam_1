package com.team1.cityfarm.dto;

import com.team1.cityfarm.entity.Payment;
import com.team1.cityfarm.entity.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class PaymentResponseDto {

    private Long paymentId;
    private String portonePaymentId;
    private String merchantOrderId;
    private Integer amount;
    private PaymentStatus status;
    private LocalDateTime createdAt;

    public static PaymentResponseDto from(Payment payment) {
        return PaymentResponseDto.builder()
                .paymentId(payment.getId())
                .portonePaymentId(payment.getPortonePaymentId())
                .merchantOrderId(payment.getOrder().getMerchantOrderId())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}