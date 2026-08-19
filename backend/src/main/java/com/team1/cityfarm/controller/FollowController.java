package com.team1.cityfarm.controller;

import com.team1.cityfarm.dto.FollowRequestDto;
import com.team1.cityfarm.dto.FollowResponseDto;
import com.team1.cityfarm.dto.FollowerResponseDto;
import com.team1.cityfarm.global.response.ApiResponse;
import com.team1.cityfarm.global.security.CustomUserDetails;
import com.team1.cityfarm.service.FollowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/follows")
public class FollowController {

    private final FollowService followService;

    // F-20 팔로우 등록
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> follow(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody FollowRequestDto request
    ) {
        followService.follow(userDetails.getUserId(), request.followingId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("팔로우가 등록되었습니다."));
    }

    // F-21 팔로잉 목록 조회 (내가 팔로우하는 사람들, 최신순, 본인 것만 조회 가능)
    @GetMapping("/following")
    public ResponseEntity<ApiResponse<List<FollowResponseDto>>> getFollowingList(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "팔로잉 목록 조회에 성공했습니다.",
                followService.getFollowingList(userDetails.getUserId())
        ));
    }

    // F-21 팔로워 목록 조회 (나를 팔로우하는 사람들, 최신순, 본인 것만 조회 가능)
    @GetMapping("/followers")
    public ResponseEntity<ApiResponse<List<FollowerResponseDto>>> getFollowerList(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "팔로워 목록 조회에 성공했습니다.",
                followService.getFollowerList(userDetails.getUserId())
        ));
    }

    // F-22 팔로우 취소 (본인의 팔로우만 취소 가능)
    @DeleteMapping("/{followId}")
    public ResponseEntity<ApiResponse<Void>> unfollow(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long followId
    ) {
        followService.unfollow(userDetails.getUserId(), followId);
        return ResponseEntity.ok(ApiResponse.success("팔로우가 취소되었습니다."));
    }
}
