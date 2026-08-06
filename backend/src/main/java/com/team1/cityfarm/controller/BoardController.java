package com.team1.cityfarm.controller;

import com.team1.cityfarm.dto.BoardRequestDto;
import com.team1.cityfarm.dto.BoardResponseDto;
import com.team1.cityfarm.global.response.ApiResponse;
import com.team1.cityfarm.service.BoardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(name = "게시판 API", description = "게시글 조회, 등록, 수정, 삭제 API")
@RestController
@RequestMapping("/api/board")
@RequiredArgsConstructor
public class BoardController {
    private final BoardService boardService;

    // 게시글 목록조회
    @GetMapping
    public ApiResponse<Page<BoardResponseDto>> getBoards(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String keyword,   // 없으면 전체조회
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ApiResponse.success("게시글 목록 조회 성공", boardService.getBoards(type, keyword, pageable));
    }

    //게시글 상제조회
    @Operation(summary = "게시글 상세 조회",
            description = "게시글 ID(boardId)를 통해 특정 게시글의 상세 정보를 조회합니다.")
    @GetMapping("/{boardId}")
    public ApiResponse<BoardResponseDto> getBoard(@PathVariable Long boardId) {
        return ApiResponse.success("게시글 조회 성공", boardService.getBoard(boardId));
    }

    // 게시글 등록
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<BoardResponseDto> createBoard(
            @Valid @RequestBody BoardRequestDto request
            // userId는 인증 붙으면 @AuthenticationPrincipal 등에서 꺼내기
    ) {
        Long userId = null; // TODO: 인증 붙으면 교체
        return ApiResponse.success("게시글 등록 성공", boardService.createBoard(request, userId));
    }

    // 게시글 수정
    @PutMapping("/{boardId}")
    public ApiResponse<BoardResponseDto> updateBoard(
            @PathVariable Long boardId,
            @Valid @RequestBody BoardRequestDto request
    ) {
        Long userId = null; // TODO
        return ApiResponse.success("게시글 수정 성공", boardService.updateBoard(boardId, request, userId));
    }

    // 게시글 삭제
    @DeleteMapping("/{boardId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBoard(@PathVariable Long boardId) {
        Long userId = null; // TODO
        boardService.deleteBoard(boardId, userId);
    }
}