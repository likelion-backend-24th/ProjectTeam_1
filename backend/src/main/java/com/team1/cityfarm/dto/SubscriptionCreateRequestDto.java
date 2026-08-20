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
    private String billingKey; // 포트원 프론트엔드 SDK에서 발급받은 빌링키 추가
}