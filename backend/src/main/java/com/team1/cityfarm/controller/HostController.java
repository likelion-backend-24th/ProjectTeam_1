package com.team1.cityfarm.controller;

import com.team1.cityfarm.dto.HostFarmResponseDto;
import com.team1.cityfarm.dto.HostReservationSummaryDto;
import com.team1.cityfarm.dto.RentalResponseDto;
import com.team1.cityfarm.dto.SettlementResponseDto;
import com.team1.cityfarm.global.response.ApiResponse;
import com.team1.cityfarm.global.security.user.CustomUserDetails;
import com.team1.cityfarm.service.FarmService;
import com.team1.cityfarm.service.RentalService;
import com.team1.cityfarm.service.SettlementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "호스트 API", description = "호스트 전용 API입니다.")
@SecurityRequirement(name = "BearerAuth")
@RestController
@RequestMapping("/api/host")
@RequiredArgsConstructor
public class HostController {

    private final SettlementService settlementService;
    private final RentalService rentalService;
    private final FarmService farmService;

    /**
     * [호스트] 내 정산내역 목록 조회
     */
    @Operation(summary = "내 정산내역 조회",
            description = "로그인한 호스트의 정산 내역 리스트를 최신순으로 조회합니다.")
    @GetMapping("/settlements")
    public ResponseEntity<List<SettlementResponseDto>> getMySettlements(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long hostId = userDetails.getUserId();
        List<SettlementResponseDto> response = settlementService.getHostSettlements(hostId);
        return ResponseEntity.ok(response);
    }

    // 호스트 내 밭을 임대한 사람들 목록 조회
    @Operation(summary = "내 밭을 임대한 현황 조회",
    description = "내가 등록한 밭들을 임대한 사람들의 목록을 최신순으로 조회")
    @GetMapping("/rentals")
    public ApiResponse<Page<RentalResponseDto>> getMyFarmRentals(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Long hostId = userDetails.getUserId();
        return  ApiResponse.success("밭 임대 현황 조회 성공", rentalService.getHostRentals(hostId, pageable));
    }

    @Operation(summary = "내가 등록한 밭 목록 조회", description = "호스트 관리 화면에서 내가 등록한 밭들을 조회합니다.")
    @GetMapping("/farms")
    public ApiResponse<List<HostFarmResponseDto>> getMyFarms(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Long hostId = userDetails.getUserId();
        return ApiResponse.success("내 밭 목록 조회 성공", farmService.getMyFarms(hostId));
    }

    @Operation(summary = "내 밭 전체 예약 현황 조회", description = "내가 등록한 모든 밭의 예약 현황 요약과 목록을 조회합니다.")
    @GetMapping("/reservations")
    public ApiResponse<HostReservationSummaryDto> getMyReservationSummary(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Long hostId = userDetails.getUserId();
        return ApiResponse.success("예약 현황 조회 성공", rentalService.getHostReservationSummary(hostId));
    }
}