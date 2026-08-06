package com.team1.cityfarm.controller;

import com.team1.cityfarm.dto.ProfileResponseDto;
import com.team1.cityfarm.entity.User;
import com.team1.cityfarm.global.response.ApiResponse;
import com.team1.cityfarm.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "프로필 API", description = "내 프로필 조회 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/profile")
public class ProfileController {
    private final ProfileService profileService;

    @Operation(summary = "내 프로필 조회",
            description = "jwt에 저장된 userId를 이용해 내 프로필 정보를 불러옵니다.",
            security = @SecurityRequirement(name = "BearerAuth") // Swagger 상단 자물쇠/토큰 표시
    )
    @GetMapping
    public ApiResponse<ProfileResponseDto> getMyProfile(
            @AuthenticationPrincipal CustomUserDeatil customUserDeatil
    ) {
        User user = customUserDetails.getUser();

        return ApiResponse.success("프로필 조회 성공", new ProfileResponseDto(user));
    }
}
