package com.team1.cityfarm.controller;

import com.team1.cityfarm.dto.FarmRequestDto;
import com.team1.cityfarm.dto.FarmResponseDto;
import com.team1.cityfarm.dto.FarmUpdateRequestDto;
import com.team1.cityfarm.global.response.ApiResponse;
import com.team1.cityfarm.global.security.user.CustomUserDetails;
import com.team1.cityfarm.service.FarmService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "밭 임대 API", description = "밭 등록, 조회, 임대 신청 API 및 HOST만 권한 부여")
@RestController
@RequestMapping("/api/farms")
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

    // 밭 목록 조회 (F-161)
    @Operation(summary = "밭 목록 조회",
            description = "등록된 밭 목록을 조회합니다. 비회원도 조회 가능")
    @GetMapping
    public ApiResponse<Page<FarmResponseDto>> getFarms(
            @PageableDefault(size = 10, sort = "createdAt",direction = Sort.Direction.DESC) Pageable pageable
    ){
        return ApiResponse.success("밭 목록 조회 성공", farmService.getFarms(pageable));
    }

    // 밭 상세 조회 (F-162)
    @Operation(summary = "밭 상세 조회",description = "밭 ID로 상세 조회하며, 비회원도 조회 가능")
    @GetMapping("/{farmId}")
    public ApiResponse<FarmResponseDto> getFarm(@PathVariable Long farmId){
        return ApiResponse.success("밭 상세 조회 성공", farmService.getFarm(farmId));
    }

    // 밭 정보 수정 (F-165)
    @Operation(summary = "밭 정보 수정", security = @SecurityRequirement(name = "BearerAuth"))
    @PutMapping(value = "/{farmId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<FarmResponseDto> updateFarm(
            @PathVariable Long farmId,
            @RequestPart("request") @Valid FarmUpdateRequestDto requestDto,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            @AuthenticationPrincipal CustomUserDetails userDetails
            ){
        Long userId = userDetails.getUserId();
        return ApiResponse.success("밭 정보 수정 성공", farmService.updateFarm(farmId, requestDto, images, userId));
    }
}
