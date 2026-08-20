package com.team1.cityfarm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;

@Schema(description = "밭 등록 및 수정 요청 DTO")
public record FarmRequestDto(

    @Schema(description = "밭 이름", example = "물 가까운 명당 자리")
    @NotBlank(message = "밭 이름은 필수입니다.")
    String title,

    @Schema(description = "지역(자동완성)", example = "xx동")
    @NotBlank(message = "지역은 필수입니다.")
    String location,

    @Schema(description = "상세 주소", example = "계곡 옆")
    String locationAddress,

    @Schema(description = "면적", example = "33")
    @NotNull(message = "면적은 필수입니다.")
    @Positive(message = "면적은 0보다 커야 합니다.")
    int area,

    @Schema(description = "월 임대료", example = "100000")
    @NotNull(message = "월 임대료는 필수입니다.")
    @Positive(message = "월 임대료는 0보다 커야 합니다.")
    int monthlyRent,

    @Schema(description = "임대 기간", example = "6")
    @NotNull(message = "임대 기간은 필수입니다.")
    @Positive(message = "임대 기간은 0보다 커야 합니다.")
    int rentalMonths,

    @Schema(description = "밭 설명", example = "계곡 옆에 햇볕이 잘 듭니다.")
    String description
) {}
