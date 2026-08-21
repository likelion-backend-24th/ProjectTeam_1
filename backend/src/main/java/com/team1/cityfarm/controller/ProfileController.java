package com.team1.cityfarm.controller;

import com.team1.cityfarm.dto.*;
import com.team1.cityfarm.entity.User;
import com.team1.cityfarm.global.response.ApiResponse;
import com.team1.cityfarm.global.security.user.CustomUserDetails;
import com.team1.cityfarm.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "프로필 API", description = "내 프로필 조회 및 수정 API")
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

    @Operation(summary = "현재 비밀번호 확인",
            description = "비밀번호 변경 전, 입력한 현재 비밀번호가 올바른지 검증합니다.")
    @PostMapping("/check-password")
    public ApiResponse<Boolean> checkPassword(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestBody @Valid PasswordCheckRequestDto requestDto
    ) {
        boolean isMatch = profileService.checkCurrentPassword(customUserDetails.getUserId(), requestDto.getCurrentPassword());

        // 일치하지 않으면 서비스에서 예외를 던지거나, 여기서 false를 리턴할 수 있습니다.
        return ApiResponse.success("비밀번호 확인 성공", isMatch);
    }

    @Operation(summary = "비밀번호 변경",
            description = "새로운 비밀번호를 받아 유저의 비밀번호를 변경합니다.")
    @PatchMapping("/password")
    public ApiResponse<Void> updatePassword(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestBody @Valid PasswordChangeRequestDto requestDto
    ) {
        profileService.changePassword(customUserDetails.getUserId(), requestDto.getNewPassword());

        return ApiResponse.success("비밀번호 변경 성공", null);
    }

    @Operation(summary = "좋아요한 게시글 목록 조회",
            description = "로그인한 사용자가 좋아요한 게시글 목록을 최신순으로 조회합니다.")
    @PostMapping("/likes")
    public ApiResponse<Page<BoardResponseDto>> getLikedBoards(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<BoardResponseDto> result = profileService.getLikedBoards(customUserDetails.getUserId(), pageable);

        return ApiResponse.success("좋아요한 게시글 목록 조회 성공", result);
    }
    @Operation(summary = "호스트 승격 신청",
    description = "사업자 정보(사업자명, 사업자등록번호, 사업장주소)를 제출하면 별도 심사 없이 즉시 HOST로 전환.")
    @PostMapping("/host-promotion")
    public ApiResponse<Void> promoteToHost(@AuthenticationPrincipal CustomUserDetails customUserDetails,
                                           @RequestBody @Valid HostPromotionRequestDto requestDto){

        profileService.promoteToHost(customUserDetails.getUserId(), requestDto);

        return ApiResponse.success("호스트 전환 완료");
    }
}