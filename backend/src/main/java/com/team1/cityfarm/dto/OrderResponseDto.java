package com.team1.cityfarm.dto;

import com.team1.cityfarm.entity.Order;
import com.team1.cityfarm.entity.OrderStatus;
import com.team1.cityfarm.entity.OrderType;
import com.team1.cityfarm.entity.Payment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class OrderResponseDto {

    private Long id;
    private String merchantOrderId;
    private OrderType orderType;
    private OrderStatus orderStatus;
    private Integer amount;
    private Integer originalAmount;
    private String title;
    private LocalDateTime scheduledAt;
    private LocalDateTime createdAt;
    private PaymentDto payment;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class PaymentDto {
        private Long id;
        private String status;
        private String payMethod;
        private LocalDateTime approvedAt;
        private LocalDateTime cancelledAt;
        private String cancelReason;

        public static PaymentDto from(Payment payment) {
            if (payment == null) return null;
            return PaymentDto.builder()
                    .id(payment.getId())
                    .status(payment.getStatus().name())
                    .payMethod("CARD")
                    .approvedAt(payment.getCreatedAt())
                    .cancelledAt(payment.getCancelledAt())
                    .cancelReason(payment.getCancelReason())
                    .build();
        }
    }

    // 단일 Order 변환 (기본값 매핑)
    public static OrderResponseDto from(Order order) {
        return OrderResponseDto.builder()
                .id(order.getId())
                .merchantOrderId(order.getMerchantOrderId())
                .orderType(order.getOrderType())
                .orderStatus(order.getOrderStatus())
                .amount(order.getAmount())
                .originalAmount(order.getAmount())
                .title("원데이클래스 신청")
                .createdAt(order.getCreatedAt())
                .build();
    }

    // 연관 객체(Payment, Class)가 포함된 상세 변환
    public static OrderResponseDto from(Order order, String title, LocalDateTime scheduledAt, Payment payment) {
        return OrderResponseDto.builder()
                .id(order.getId())
                .merchantOrderId(order.getMerchantOrderId())
                .orderType(order.getOrderType())
                .orderStatus(order.getOrderStatus())
                .amount(order.getAmount())
                .originalAmount(order.getAmount())
                .title(title)
                .scheduledAt(scheduledAt)
                .createdAt(order.getCreatedAt())
                .payment(PaymentDto.from(payment))
                .build();
    }
}