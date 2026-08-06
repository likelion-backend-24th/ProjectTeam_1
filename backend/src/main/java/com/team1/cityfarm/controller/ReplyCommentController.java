package com.team1.cityfarm.controller;

import com.team1.cityfarm.dto.ReplyCommentRequestDto;
import com.team1.cityfarm.dto.ReplyCommentResponseDto;
import com.team1.cityfarm.global.response.ApiResponse;
import com.team1.cityfarm.global.security.CustomUserDetails;
import com.team1.cityfarm.service.ReplyCommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Reply Comment", description = "답변 댓글 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ReplyCommentController {

    private final ReplyCommentService replyCommentService;

    // 답변 댓글 등록
    @Operation(summary = "답변 댓글 등록",
            description = "지정한 답변에 새로운 댓글을 작성합니다.",
            security = @SecurityRequirement(name = "BearerAuth"))
    @PostMapping("/reply/{replyId}/reply-comments")
    public ResponseEntity<ApiResponse<ReplyCommentResponseDto>> createComment(
            @PathVariable Long replyId,
            @Valid @RequestBody ReplyCommentRequestDto requestDto,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getUser().getId();
        ReplyCommentResponseDto result = replyCommentService.createComment(replyId, requestDto, userId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("답변 댓글이 등록되었습니다.", result));
    }

    // 답변 댓글 목록 조회
    @Operation(summary = "답변 댓글 목록 조회",
            description = "특정 답변에 달린 모든 댓글 목록을 조회합니다.")
    @GetMapping("/reply/{replyId}/reply-comments")
    public ResponseEntity<ApiResponse<List<ReplyCommentResponseDto>>> getComments(
            @PathVariable Long replyId
    ) {
        List<ReplyCommentResponseDto> result = replyCommentService.getComments(replyId);

        return ResponseEntity.ok(ApiResponse.success("답변 댓글 목록 조회 성공", result));
    }

    // 답변 댓글 수정
    @Operation(summary = "답변 댓글 수정",
            description = "본인이 작성한 답변 댓글의 내용을 수정합니다.",
            security = @SecurityRequirement(name = "BearerAuth"))
    @PatchMapping("/reply-comments/{commentId}")
    public ResponseEntity<ApiResponse<ReplyCommentResponseDto>> updateComment(
            @PathVariable Long commentId,
            @Valid @RequestBody ReplyCommentRequestDto requestDto,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getUser().getId();
        ReplyCommentResponseDto result = replyCommentService.updateComment(commentId, requestDto, userId);

        return ResponseEntity.ok(ApiResponse.success("답변 댓글이 수정되었습니다.", result));
    }

    // 답변 댓글 삭제
    @Operation(summary = "답변 댓글 삭제",
            description = "본인이 작성한 답변 댓글을 삭제합니다.",
            security = @SecurityRequirement(name = "BearerAuth"))
    @DeleteMapping("/reply-comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable Long commentId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getUser().getId();
        replyCommentService.deleteComment(commentId, userId);

        return ResponseEntity.ok(ApiResponse.success("답변 댓글이 삭제되었습니다."));
    }
}