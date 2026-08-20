package com.team1.cityfarm.controller;

import com.team1.cityfarm.dto.FarmRequestDto;
import com.team1.cityfarm.dto.FarmResponseDto;
import com.team1.cityfarm.global.response.ApiResponse;
import com.team1.cityfarm.global.security.user.CustomUserDetails;
import com.team1.cityfarm.service.FarmService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "밭 임대 API", description = "밭 등록, 조회, 임대 신청 API 및 HOST만 권한 부여")
@RestController
@RequestMapping("/api/farm")
@RequiredArgsConstructor
public class FarmController {
    private final FarmService farmService;

    // 밭 등록 (F-160)
    @Operation(summary = "밭 임대",
        description = "HOST가 임대 가능한 밭을 등록합니다.",
        security = @SecurityRequirement(name = "BearerAuth"))
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<FarmResponseDto> createFarm(
            @RequestPart("request") @Valid FarmRequestDto requestDto,
            @RequestPart(value = "images", required = false)List<MultipartFile>images,
            @AuthenticationPrincipal CustomUserDetails userDetails
            ) {
                Long userId = userDetails.getUserId();
                return ApiResponse.success("밭 등록 성공", farmService.createFarm(requestDto, images, userId));
    }
}
