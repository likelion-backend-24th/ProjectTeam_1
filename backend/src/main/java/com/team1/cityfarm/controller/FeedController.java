package com.team1.cityfarm.controller;

import com.team1.cityfarm.dto.BoardResponseDto;
import com.team1.cityfarm.global.response.ApiResponse;
import com.team1.cityfarm.global.security.CustomUserDetails;
import com.team1.cityfarm.service.FeedService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "피드 API", description = "팔로우한 사용자의 게시글 피드 조회 API")
@SecurityRequirement(name = "BearerAuth")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/feed")
public class FeedController {

    private final FeedService feedService;

    // F-30 피드 목록 조회 (내가 팔로우한 사용자들의 게시글, 최신순)
    @Operation(summary = "피드 목록 조회",
            description = "로그인한 사용자가 팔로우한 사용자들의 게시글 목록을 최신순으로 조회합니다.")
    @GetMapping
    public ApiResponse<Page<BoardResponseDto>> getFeed(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ApiResponse.success(
                "피드 목록 조회에 성공했습니다.",
                feedService.getFeed(userDetails.getUserId(), pageable)
        );
    }
}
