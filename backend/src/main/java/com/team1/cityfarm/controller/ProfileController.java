package com.team1.cityfarm.controller;

import com.team1.cityfarm.dto.ProfileResponseDto;
import com.team1.cityfarm.global.response.ApiResponse;
import com.team1.cityfarm.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/profile")
public class ProfileController {
    private final ProfileService profileService;

    @GetMapping
    public ApiResponse<ProfileResponseDto> getMyProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        // JWT 필터를 통해 인증된 유저의 ID 추출
        Long currentUserId = userDetails.getUserId();

        User user = profileService.getUserProfile(currentUserId);

        return ApiResponse.success("프로필 조회 성공", new ProfileResponseDto(user));
    }
}
