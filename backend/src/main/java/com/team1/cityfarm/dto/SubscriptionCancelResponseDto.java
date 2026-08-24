package com.team1.cityfarm.dto;

import com.team1.cityfarm.entity.Subscription;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionCancelResponseDto {

    private SubscriptionCancelResultType resultType;
    private SubscriptionResponseDto subscription;
    // resultType이 REFUNDED일 때만 값이 채워진다.
    private Integer refundedAmount;

    public static SubscriptionCancelResponseDto of(
            SubscriptionCancelResultType resultType, Subscription subscription, Integer refundedAmount
    ) {
        return SubscriptionCancelResponseDto.builder()
                .resultType(resultType)
                .subscription(SubscriptionResponseDto.from(subscription))
                .refundedAmount(refundedAmount)
                .build();
    }
}
