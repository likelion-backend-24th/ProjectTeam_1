package com.team1.cityfarm.dto;

import com.team1.cityfarm.entity.Farm;
import com.team1.cityfarm.entity.FarmStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "호스트 - 내 밭 목록(관리용) 응답 DTO")
public record HostFarmResponseDto(

        @Schema(description = "밭 Id", example = "1")
        Long id,

        @Schema(description = "밭 이름", example = "햇살 좋은 주말농장")
        String title,

        @Schema(description = "지역명", example = "서울 xx동")
        String location,

        @Schema(description = "대표 사진 URL", example = "/uploads/farms/xxxx.jpg")
        String thumbnailUrl,

        @Schema(description = "임대 상태", example = "AVAILABLE")
        FarmStatus farmStatus,

        @Schema(description = "이 밭의 확정된 임대 건수", example = "1")
        long rentalCount
) {
    public static HostFarmResponseDto from(Farm farm, String thumbnailUrl, long rentalCount) {
        return new HostFarmResponseDto(
                farm.getId(),
                farm.getTitle(),
                farm.getLocation(),
                thumbnailUrl,
                farm.getFarmStatus(),
                rentalCount
        );
    }
}
