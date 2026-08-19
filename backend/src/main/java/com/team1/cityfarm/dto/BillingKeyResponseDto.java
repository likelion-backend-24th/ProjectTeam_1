package com.team1.cityfarm.dto;

import com.team1.cityfarm.entity.BillingKey;
import com.team1.cityfarm.entity.BillingKeyStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillingKeyResponseDto {

    private Long id;
    private BillingKeyStatus status;
    private LocalDateTime issuedAt;
    private LocalDateTime expiredAt;
    private LocalDateTime createdAt;

    public static BillingKeyResponseDto from(BillingKey billingKey) {
        if (billingKey == null) return null;
        return BillingKeyResponseDto.builder()
                .id(billingKey.getId())
                .status(billingKey.getStatus())
                .issuedAt(billingKey.getIssuedAt())
                .expiredAt(billingKey.getExpiredAt())
                .createdAt(billingKey.getCreatedAt())
                .build();
    }
}