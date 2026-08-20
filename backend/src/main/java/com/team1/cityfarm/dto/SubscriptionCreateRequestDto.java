package com.team1.cityfarm.dto;

import com.team1.cityfarm.entity.SubscriptionPlanType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionCreateRequestDto {

    private SubscriptionPlanType planType;
    private Long billingKeyId; // 프론트가 이미 이 이름으로 보내고 있음 — 우리 DB의 BillingKey row id
}