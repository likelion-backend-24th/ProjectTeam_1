package com.team1.cityfarm.controller;

import com.team1.cityfarm.dto.BoardRequestDto;
import com.team1.cityfarm.dto.BoardResponseDto;
import com.team1.cityfarm.service.BoardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@Tag(name = "게시판 API", description = "게시글 조회, 등록, 수정, 삭제 API")
@RestController
@RequestMapping("/api/board")
@RequiredArgsConstructor
public class BoardController {
    private final BoardService boardService;

    @Operation(summary = "게시판 목록 조회",
            description = "게시판 목록을 받아옵니다.")
    @GetMapping
    public Page<BoardResponseDto> getBoards(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String keyword,   // 없으면 전체조회
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return boardService.getBoards(type, keyword, pageable);
    }

    @GetMapping("/{boardId}")
    public BoardResponseDto getBoard(
            @PathVariable Long boardId
    ) {
        return boardService.getBoard(boardId);
    }

    @PostMapping
    public BoardResponseDto createBoard(
            @RequestBody BoardRequestDto request,
            @RequestParam Long userId
    ) {
        return boardService.createBoard(request, userId);
    }
}
