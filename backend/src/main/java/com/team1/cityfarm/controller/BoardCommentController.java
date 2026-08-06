package com.team1.cityfarm.controller;

import com.team1.cityfarm.dto.BoardCommentRequestDto;
import com.team1.cityfarm.dto.BoardCommentResponseDto;
import com.team1.cityfarm.global.response.ApiResponse;
import com.team1.cityfarm.service.BoardCommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class BoardCommentController {

    private final BoardCommentService boardCommentService;

    //    게시글 댓글 등록
    @PostMapping("/board/{boardId}/board-comments")
    public ResponseEntity<ApiResponse<BoardCommentResponseDto>> createComment(@PathVariable Long boardId,
                                                                              @Valid @RequestBody BoardCommentRequestDto requestDto
            /* TODO(JWT),@AuthenticationPrincipal CustomUserDetails userDetails 파라미터 추가*/) {

        Long userId = 1L; // TODO: userDetails.getUserId() 로 교체
        BoardCommentResponseDto result = boardCommentService.createComment(boardId, requestDto, userId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("댓글이 등록되었습니다.", result));

    }

    //    게시글 댓글 목록 조회
    @GetMapping("/board/{boardId}/board-comments")
    public ResponseEntity<ApiResponse<List<BoardCommentResponseDto>>> getComments(@PathVariable Long boardId) {

        List<BoardCommentResponseDto> result = boardCommentService.getComments(boardId);

        return ResponseEntity.ok(ApiResponse.success("댓글 목록 조회 성공", result));
    }

    //    게시글 댓글 수정
    @PatchMapping("/board-comments/{commentId}")
    public ResponseEntity<ApiResponse<BoardCommentResponseDto>> updateComment(
            @PathVariable Long commentId,
            @Valid @RequestBody BoardCommentRequestDto request
            // TODO: JWT 붙으면 @AuthenticationPrincipal CustomUserDetails userDetails 추가
    ) {

        Long userId = 1L; // TODO: userDetails.getUserId()로 교체
        BoardCommentResponseDto result = boardCommentService.updateComment(commentId, request, userId);

        return ResponseEntity.ok(ApiResponse.success("댓글이 수정되었습니다.", result));
    }

    //게시글 댓글 삭제
    @DeleteMapping("/board-comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable Long commentId
            // TODO: JWT 붙으면 @AuthenticationPrincipal CustomUserDetails userDetails 파라미터 추가
    ) {

        Long userId = 1L; // TODO: userDetails.getUserId()로 교체
        boardCommentService.deleteComment(commentId, userId);

        return ResponseEntity.ok(ApiResponse.success("댓글이 삭제되었습니다."));
    }

}
