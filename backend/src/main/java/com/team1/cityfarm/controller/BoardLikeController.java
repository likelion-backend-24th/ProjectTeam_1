package com.team1.cityfarm.controller;

import com.team1.cityfarm.dto.BoardLikeResponseDto;
import com.team1.cityfarm.global.response.ApiResponse;
import com.team1.cityfarm.global.security.CustomUserDetails;
import com.team1.cityfarm.service.BoardLikeService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Board Like", description = "게시글 좋아요 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/board/{boardId}/likes")
public class BoardLikeController {

    private final BoardLikeService boardLikeService;

    //좋아요 추가
    @PostMapping
    public ApiResponse<BoardLikeResponseDto> likeBoard(
            @PathVariable Long boardId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getUserId();
        BoardLikeResponseDto result = boardLikeService.likeBoard(boardId, userId);

        return ApiResponse.success("게시글 좋아요가 등록되었습니다.", result);
    }

    //좋아요 취소
    @DeleteMapping
    public ResponseEntity<ApiResponse<BoardLikeResponseDto>> unlikeBoard(
            @PathVariable Long boardId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getUserId();
        BoardLikeResponseDto result = boardLikeService.unlikeBoard(boardId, userId);

        return ResponseEntity.ok(ApiResponse.success("게시글 좋아요가 취소되었습니다.", result));
    }
}
