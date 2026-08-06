package com.team1.cityfarm.controller;

import com.team1.cityfarm.dto.ReplyCommentRequestDto;
import com.team1.cityfarm.dto.ReplyCommentResponseDto;
import com.team1.cityfarm.global.response.ApiResponse;
import com.team1.cityfarm.service.ReplyCommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ReplyCommentController {

    private final ReplyCommentService replyCommentService;

    // 답변 댓글 등록
    @PostMapping("/reply/{replyId}/reply-comments")
    public ResponseEntity<ApiResponse<ReplyCommentResponseDto>> createComment(
            @PathVariable Long replyId,
            @Valid @RequestBody ReplyCommentRequestDto requestDto
            // TODO: JWT 붙으면 @AuthenticationPrincipal CustomUserDetails userDetails 추가
    ) {
        Long userId = 1L; // TODO: userDetails.getUserId()로 교체
        ReplyCommentResponseDto result = replyCommentService.createComment(replyId, requestDto, userId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("답변 댓글이 등록되었습니다.", result));
    }

    // 답변 댓글 목록 조회
    @GetMapping("/reply/{replyId}/reply-comments")
    public ResponseEntity<ApiResponse<List<ReplyCommentResponseDto>>> getComments(
            @PathVariable Long replyId
    ) {
        List<ReplyCommentResponseDto> result = replyCommentService.getComments(replyId);

        return ResponseEntity.ok(ApiResponse.success("답변 댓글 목록 조회 성공", result));
    }

    // 답변 댓글 수정
    @PatchMapping("/reply-comments/{commentId}")
    public ResponseEntity<ApiResponse<ReplyCommentResponseDto>> updateComment(
            @PathVariable Long commentId,
            @Valid @RequestBody ReplyCommentRequestDto requestDto
            // TODO: JWT 붙으면 @AuthenticationPrincipal CustomUserDetails userDetails 추가
    ) {
        Long userId = 1L; // TODO: userDetails.getUserId()로 교체
        ReplyCommentResponseDto result = replyCommentService.updateComment(commentId, requestDto, userId);

        return ResponseEntity.ok(ApiResponse.success("답변 댓글이 수정되었습니다.", result));
    }

    // 답변 댓글 삭제
    @DeleteMapping("/reply-comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable Long commentId
            // TODO: JWT 붙으면 @AuthenticationPrincipal CustomUserDetails userDetails 추가
    ) {
        Long userId = 1L; // TODO: userDetails.getUserId()로 교체
        replyCommentService.deleteComment(commentId, userId);

        return ResponseEntity.ok(ApiResponse.success("답변 댓글이 삭제되었습니다."));
    }
}