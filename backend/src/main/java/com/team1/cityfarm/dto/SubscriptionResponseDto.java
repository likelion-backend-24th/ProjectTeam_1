package com.team1.cityfarm.dto;

import com.team1.cityfarm.entity.Subscription;
import com.team1.cityfarm.entity.SubscriptionPlanType;
import com.team1.cityfarm.entity.SubscriptionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionResponseDto {

    private Long id;
    private Long userId;
    private SubscriptionPlanType planType;
    private SubscriptionStatus status;
    private LocalDateTime currentPeriodStart;
    private LocalDateTime currentPeriodEnd;
    private boolean cancelAtPeriodEnd;
    private LocalDateTime cancelledAt;
    private LocalDateTime createdAt;

    public static SubscriptionResponseDto from(Subscription subscription) {
        if (subscription == null) return null;
        return SubscriptionResponseDto.builder()
                .id(subscription.getId())
                .userId(subscription.getUser() != null ? subscription.getUser().getId() : null)
                .planType(subscription.getPlanType())
                .status(subscription.getStatus())
                .currentPeriodStart(subscription.getCurrentPeriodStart())
                .currentPeriodEnd(subscription.getCurrentPeriodEnd())
                .cancelAtPeriodEnd(subscription.isCancelAtPeriodEnd())
                .cancelledAt(subscription.getCancelledAt())
                .createdAt(subscription.getCreatedAt())
                .build();
    }
}