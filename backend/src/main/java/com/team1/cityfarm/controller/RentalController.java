package com.team1.cityfarm.controller;

import com.team1.cityfarm.dto.RentalRequestDto;
import com.team1.cityfarm.dto.RentalResponseDto;
import com.team1.cityfarm.global.response.ApiResponse;
import com.team1.cityfarm.global.security.user.CustomUserDetails;
import com.team1.cityfarm.service.RentalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class RentalController {
    private final RentalService rentalService;

    // 밭 임대 신청 취소 (F-164)
    @Operation(summary = "밭 임대 신청 취소", security = @SecurityRequirement(name = "BearerAuth"))
    @DeleteMapping("/api/rentals/{rentalId}")
    public ApiResponse<Void> cancelRental(
            @PathVariable Long rentalId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ){
        Long userId = userDetails.getUserId();
        rentalService.cancelRental(rentalId, userId);
        return ApiResponse.success("밭 임대 신청 취소 성공");
    }

    // 마이페이지
    @Operation(summary = "내 임대 예약 목록", security = @SecurityRequirement(name = "BearerAuth"))
    @GetMapping("/api/rentals/me")
    public ApiResponse<Page<RentalResponseDto>> getMyRentals(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ){
        return ApiResponse.success("내 임대 예약 목록 조회 성공", rentalService.getMyRentals(userDetails.getUserId(), pageable));
    }
}
