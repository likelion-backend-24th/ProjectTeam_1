package com.team1.cityfarm.dto;

import com.team1.cityfarm.entity.Rental;
import com.team1.cityfarm.entity.RentalStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "임대 신청 응답 DTO")
public record RentalResponseDto (

    @Schema(description = "신청 ID", example = "1")
    Long id,

    @Schema(description = "밭 ID", example = "1")
    Long farmId,

    @Schema(description = "밭 매물 제목", example = "물 가까운")
    String farmTitle,

    @Schema(description = "지역명", example = "서울 xx동")
    String location,

    @Schema(description = "신청자 닉네임", example = "초보 농부")
    String userNickname,

    @Schema(description = "밭 설명", example = "계곡 옆에 햇볕이 잘 듭니다.")
    String description,

    @Schema(description = "신청 상태", example = "REQUESTED")
    RentalStatus rentalStatus,

    @Schema(description = "임대 시작 (신청일 기준)", example = "2026-09-22T15:00:00")
    String rentalStart,

    @Schema(description = "임대 종료 (임대 시작 + 밭의 임대 개월수)", example = "2026-10-22T15:00:00")
    String rentalEnd,

    @Schema(description = "신청 일시", example = "2026-08-00")
    String createdAt,

    @Schema(description = "상태 변경 일시", example = "2026-08-00")
    String updatedAt,

    @Schema(description = "밭 등록자 닉네임", example = "농장주 홍길동")
    String hostNickname
){
    public static RentalResponseDto from(Rental rental){
        LocalDateTime rentalStart = rental.getCreatedAt();
        LocalDateTime rentalEnd = rentalStart.plusMonths(rental.getFarm().getRentalMonths());
        return new RentalResponseDto(
                rental.getId(),
                rental.getFarm().getId(),
                rental.getFarm().getTitle(),
                rental.getFarm().getLocation(),
                rental.getUser().getNickname(),
                rental.getDescription(),
                rental.getRentalStatus(),
                rentalStart.toString(),
                rentalEnd.toString(),
                rental.getCreatedAt().toString(),
                rental.getUpdatedAt().toString(),
                rental.getFarm().getUser().getNickname()
        );
    }
}
