package com.team1.cityfarm.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "호스트 - 전체 예약 현황 요약 DTO")
public record HostReservationSummaryDto(

        @Schema(description = "예약 확정 건수(내 모든 밭 기준)", example = "3")
        long confirmedCount,

        @Schema(description = "오늘 입주(임대 시작) 건수", example = "1")
        long todayCheckinCount,

        @Schema(description = "현재 이용 중인 건수", example = "2")
        long inUseCount,

        @Schema(description = "이번 달 신청 건수", example = "4")
        long thisMonthCount,

        @Schema(description = "예약 목록 (최신순)")
        List<RentalResponseDto> rentals
) {
    public static HostReservationSummaryDto of(
            long confirmedCount, long todayCheckinCount, long inUseCount, long thisMonthCount,
            List<RentalResponseDto> rentals
    ) {
        return new HostReservationSummaryDto(confirmedCount, todayCheckinCount, inUseCount, thisMonthCount, rentals);
    }
}
