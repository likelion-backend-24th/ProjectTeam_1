package com.team1.cityfarm.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "밭 임대 요청 DTO")
public record RentalRequestDto(
        @Schema(description = "신청 시 남기는 메시지", example = "아이들과 주말 농장을 하려고 신청합니다!")
        String description
) {}
