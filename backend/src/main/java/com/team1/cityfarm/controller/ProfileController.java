package com.team1.cityfarm.controller;

import com.team1.cityfarm.dto.ProfileRequestDto;
import com.team1.cityfarm.dto.ProfileResponseDto;
import com.team1.cityfarm.entity.User;
import com.team1.cityfarm.global.response.ApiResponse;
import com.team1.cityfarm.global.security.user.CustomUserDetails;
import com.team1.cityfarm.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "프로필 API", description = "내 프로필 조회 API")
@SecurityRequirement(name = "BearerAuth")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileService profileService;

    @Operation(summary = "내 프로필 조회",
            description = "jwt에 저장된 userId를 이용해 내 프로필 정보를 불러옵니다.")
    @GetMapping
    public ApiResponse<ProfileResponseDto> getMyProfile(
            @AuthenticationPrincipal CustomUserDetails customUserDetails
            ) {
        User user = profileService.getUser(customUserDetails.getUserId());

        return ApiResponse.success("프로필 조회 성공", new ProfileResponseDto(user));
    }

    @Operation(summary = "내 프로필 수정",
            description = "jwt에 저장된 userId와 새로운 닉네임 정보를 받아 프로필을 수정합니다.")
    @PatchMapping
    public ApiResponse<Void> updateMyProfile(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestBody @Valid ProfileRequestDto profileRequestDto
            ) {
        profileService.updateProfile(customUserDetails.getUserId(), profileRequestDto.getNickname());

        return ApiResponse.success("프로필 수정 성공", null);
    }
}
