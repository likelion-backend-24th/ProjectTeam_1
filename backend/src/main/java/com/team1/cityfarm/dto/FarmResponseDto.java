package com.team1.cityfarm.dto;

import com.team1.cityfarm.entity.Farm;
import com.team1.cityfarm.entity.FarmStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record FarmResponseDto(

        @Schema(description = "밭 Id", example = "1")
        Long id,

        @Schema(description = "판매자 닉네임", example = "농부김씨")
        String ownerNickname,

        @Schema(description = "밭 이름", example = "햇살 좋은 주말")
        String title,

        @Schema(description = "지역명", example = "서울 xx동")
        String location,

        @Schema(description = "상세 주소", example = "계곡 옆")
        String locationAddress,

        @Schema(description = "면적", example = "33")
        int area,

        @Schema(description = "월 임대료", example = "100000")
        int monthlyRent,

        @Schema(description = "임대 기간", example = "6")
        int rentalMonths,

        @Schema(description = "대표 사진 URL", example = "/uploads/farms/xxxx.jpg")
        String thumbnailUrl,

        @Schema(description = "전체 사진 URL 목록 (상세 조회 시에만 채워짐)")
        List<String> imageUrls,

        @Schema(description = "밭 설명", example = "계곡 옆에 햇볕이 잘 듭니다.")
        String description,

        @Schema(description = "임대 상태", example = "임대 가능")
        FarmStatus farmStatus,

        @Schema(description = "등록 일시", example = "2026-08-19")
        String createdAt
) {
    public static FarmResponseDto from(Farm farm, String thumbnailUrl, List<String> imageUrls) {
        return new FarmResponseDto(
                farm.getId(),
                farm.getUser().getNickname(),
                farm.getTitle(),
                farm.getLocation(),
                farm.getLocationAddress(),
                farm.getArea(),
                farm.getMonthlyRent(),
                farm.getRentalMonths(),
                thumbnailUrl,
                imageUrls,
                farm.getDescription(),
                farm.getFarmStatus(),
                farm.getCreatedAt().toString()
        );
    }
}
