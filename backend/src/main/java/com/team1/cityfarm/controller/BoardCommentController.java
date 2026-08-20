package com.team1.cityfarm.controller;

import com.team1.cityfarm.dto.BoardCommentRequestDto;
import com.team1.cityfarm.dto.BoardCommentResponseDto;
import com.team1.cityfarm.global.response.ApiResponse;
import com.team1.cityfarm.global.security.user.CustomUserDetails;
import com.team1.cityfarm.service.BoardCommentService;
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

@Tag(name = "게시글 댓글 API", description = "게시글 댓글 조회, 등록, 수정, 삭제 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class BoardCommentController {

    private final BoardCommentService boardCommentService;

    //    게시글 댓글 등록
    @Operation(summary = "게시글 댓글 등록",
            description = "지정한 게시글에 새로운 댓글을 작성합니다.",
            security = @SecurityRequirement(name = "BearerAuth"))
    @PostMapping("/board/{boardId}/board-comments")
    public ResponseEntity<ApiResponse<BoardCommentResponseDto>> createComment(@PathVariable Long boardId,
                                                                              @Valid @RequestBody BoardCommentRequestDto requestDto,
                                                                              @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long userId = userDetails.getUserId();
        BoardCommentResponseDto result = boardCommentService.createComment(boardId, requestDto, userId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("댓글이 등록되었습니다.", result));

    }

    //    게시글 댓글 목록 조회
    @Operation(summary = "게시글 댓글 목록 조회",
            description = "특정 게시글에 달린 모든 댓글 목록을 조회합니다.")
    @GetMapping("/board/{boardId}/board-comments")
    public ResponseEntity<ApiResponse<List<BoardCommentResponseDto>>> getComments(@PathVariable Long boardId) {

        List<BoardCommentResponseDto> result = boardCommentService.getComments(boardId);

        return ResponseEntity.ok(ApiResponse.success("댓글 목록 조회 성공", result));
    }

    //    게시글 댓글 수정
    @Operation(summary = "게시글 댓글 수정",
            description = "본인이 작성한 댓글의 내용을 수정합니다.",
            security = @SecurityRequirement(name = "BearerAuth"))
    @PatchMapping("/board-comments/{commentId}")
    public ResponseEntity<ApiResponse<BoardCommentResponseDto>> updateComment(
            @PathVariable Long commentId,
            @Valid @RequestBody BoardCommentRequestDto request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {

        Long userId = userDetails.getUserId();
        BoardCommentResponseDto result = boardCommentService.updateComment(commentId, request, userId);

        return ResponseEntity.ok(ApiResponse.success("댓글이 수정되었습니다.", result));
    }

    //게시글 댓글 삭제
    @Operation(summary = "게시글 댓글 삭제",
            description = "본인이 작성한 댓글을 삭제합니다.",
            security = @SecurityRequirement(name = "BearerAuth"))
    @DeleteMapping("/board-comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable Long commentId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {

        Long userId = userDetails.getUserId();
        boardCommentService.deleteComment(commentId, userId);

        return ResponseEntity.ok(ApiResponse.success("댓글이 삭제되었습니다."));
    }

}
