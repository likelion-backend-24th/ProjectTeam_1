package com.team1.cityfarm.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class FarmRentalOrderCreateRequestDto {
    private Long farmId;
    private String description;
}
