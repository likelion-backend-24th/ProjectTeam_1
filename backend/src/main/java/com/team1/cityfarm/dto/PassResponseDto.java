package com.team1.cityfarm.dto;

import com.team1.cityfarm.entity.PassStatus;
import com.team1.cityfarm.entity.SubscriptionPass;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PassResponseDto {

    private Long id;
    private Integer totalCount;
    private Integer remainingCount;
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
    private PassStatus status;

    public static PassResponseDto from(SubscriptionPass pass) {
        if (pass == null) return null;
        return PassResponseDto.builder()
                .id(pass.getId())
                .totalCount(pass.getTotalCount())
                .remainingCount(pass.getRemainingCount())
                .validFrom(pass.getValidFrom())
                .validUntil(pass.getValidUntil())
                .status(pass.getStatus())
                .build();
    }
}