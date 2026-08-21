package com.team1.cityfarm.controller;

import com.team1.cityfarm.dto.RentalRequestDto;
import com.team1.cityfarm.dto.RentalResponseDto;
import com.team1.cityfarm.global.response.ApiResponse;
import com.team1.cityfarm.global.security.user.CustomUserDetails;
import com.team1.cityfarm.service.RentalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/farms")
@RequiredArgsConstructor
public class RentalController {
    private final RentalService rentalService;

    // 밭 임대 신청 (F163)
    @Operation(summary = "밭 임대 신청", security = @SecurityRequirement(name = "BearerAuth"))
    @PostMapping("/{farmId}/rent")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RentalResponseDto> applyRental(
            @PathVariable Long farmId,
            @RequestBody(required = false)RentalRequestDto requestDto,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ){
        RentalRequestDto dto = (requestDto != null) ? requestDto : new RentalRequestDto(null);
        Long userId = userDetails.getUserId();
        return ApiResponse.success("밭 임대 신청 성공", rentalService.applyRental(farmId, dto, userId));
    }

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
}
