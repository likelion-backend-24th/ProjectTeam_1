package com.team1.cityfarm.controller;

import com.team1.cityfarm.dto.OneDayClassRequestDto;
import com.team1.cityfarm.dto.OneDayClassResponseDto;
import com.team1.cityfarm.dto.OneDayClassSummaryDto;
import com.team1.cityfarm.global.response.ApiResponse;
import com.team1.cityfarm.global.security.user.CustomUserDetails;
import com.team1.cityfarm.service.OneDayClassService;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "원데이클래스 API", description = "원데이클래스 조회, 등록 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/onedayclass")
public class OneDayClassController {

    private final OneDayClassService oneDayClassService;

    @Operation(summary = "원데이클래스 목록 조회",
            description = "원데이클래스 목록을 페이징하여 조회합니다.")
    @GetMapping
    public ApiResponse<Page<OneDayClassSummaryDto>> getClassList(
            @PageableDefault(size = 10, sort = "date", direction = Sort.Direction.ASC) Pageable pageable
    ) {

        return ApiResponse.success("클래스 목록 조회 성공", oneDayClassService.getClassList(pageable));

    }

    @Operation(summary = "원데이클래스 상세 조회",
            description = "원데이 클래스 ID를 통해 상세 정보를 조회합니다.")
    @GetMapping("/{classId}")
    public ApiResponse<OneDayClassResponseDto> getClassDetail(@PathVariable Long classId) {

        return ApiResponse.success("클래스 상세 조회 성공", oneDayClassService.getClassDetail(classId));

    }

    @Operation(summary = "원데이 클래스 등록",
            description = "호스트 권한을 가진 사용자가 원데이 클래스를 등록합니다.",
            security = @SecurityRequirement(name = "BearerAuth"))
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<OneDayClassResponseDto> createOneDayClass(@Valid @RequestBody OneDayClassRequestDto requestDto,
                                                                 @AuthenticationPrincipal CustomUserDetails customUserDetails) {

        Long hostId = customUserDetails.getUserId();

        return ApiResponse.success("클래스 등록 성공", oneDayClassService.createOneDayClass(hostId, requestDto));

    }

}
