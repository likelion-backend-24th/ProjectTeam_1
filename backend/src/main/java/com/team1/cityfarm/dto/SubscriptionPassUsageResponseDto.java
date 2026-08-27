package com.team1.cityfarm.dto;

import com.team1.cityfarm.entity.SubscriptionPassUsage;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class SubscriptionPassUsageResponseDto {

    private Long id;
    private Long classId;
    private String className;
    private LocalDateTime classDate;
    private LocalDateTime usedAt;

    public static SubscriptionPassUsageResponseDto from(SubscriptionPassUsage usage) {
        return SubscriptionPassUsageResponseDto.builder()
                .id(usage.getId())
                .classId(usage.getEnrollment().getOneDayClass().getId())
                .className(usage.getEnrollment().getOneDayClass().getTitle())
                .classDate(usage.getEnrollment().getOneDayClass().getDate())
                .usedAt(usage.getUsedAt())
                .build();
    }
}
