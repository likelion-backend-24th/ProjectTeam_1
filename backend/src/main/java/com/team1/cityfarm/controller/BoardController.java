package com.team1.cityfarm.controller;

import com.team1.cityfarm.dto.BoardRequestDto;
import com.team1.cityfarm.dto.BoardResponseDto;
import com.team1.cityfarm.service.BoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/board")
@RequiredArgsConstructor
public class BoardController {
    private final BoardService boardService;

    //게시글 목록조회
    @GetMapping
    public Page<BoardResponseDto> getBoards(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String keyword,   // 없으면 전체조회
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return boardService.getBoards(type, keyword, pageable);
    }

    //게시글 상제조회
    @GetMapping("/{boardId}")
    public BoardResponseDto getBoard(
            @PathVariable Long boardId
    ) {
        return boardService.getBoard(boardId);
    }

    //게시글 등록
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BoardResponseDto createBoard(
            @RequestBody BoardRequestDto request,
            @RequestParam Long userId
    ) {
        return boardService.createBoard(request, userId);
    }

    //게시글 수정
    @PutMapping("/{boardId}")
    public BoardResponseDto updateBoard(
            @RequestBody BoardRequestDto request,
            @PathVariable Long boardId,
            @RequestParam Long userId
    ){
        return boardService.updateBoard(boardId, request, userId);
    }

    //게시글 삭제
    @DeleteMapping("/{boardId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBoard(
            @PathVariable Long boardId,
            @RequestParam Long userId
    ){
        boardService.deleteBoard(boardId,userId);
    }
}
