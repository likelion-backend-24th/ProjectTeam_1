package com.team1.cityfarm.controller;

import com.team1.cityfarm.dto.BoardResponseDto;
import com.team1.cityfarm.service.BoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/board")
@RequiredArgsConstructor
public class BoardController {
    private final BoardService boardService;

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
}
