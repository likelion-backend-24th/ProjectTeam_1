package com.team1.cityfarm.dto;

import com.team1.cityfarm.entity.Order;
import com.team1.cityfarm.entity.OrderStatus;
import com.team1.cityfarm.entity.OrderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponseDto {

    private Long orderId;
    private String merchantOrderId;
    private Integer amount;
    private OrderType orderType;
    private OrderStatus orderStatus;
    private LocalDateTime createdAt;

    public static OrderResponseDto from(Order order) {
        return OrderResponseDto.builder()
                .orderId(order.getId())
                .merchantOrderId(order.getMerchantOrderId())
                .amount(order.getAmount())
                .orderType(order.getOrderType())
                .orderStatus(order.getOrderStatus())
                .createdAt(order.getCreatedAt())
                .build();
    }
}