package com.team1.cityfarm.controller;

import com.team1.cityfarm.dto.BoardRequestDto;
import com.team1.cityfarm.dto.BoardResponseDto;
import com.team1.cityfarm.service.BoardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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

    //게시글 목록조회
    @Operation(summary = "게시글 목록 조회",
            description = "유형(type) 및 검색어(keyword) 조건에 맞춰 페이징된 게시글 목록을 조회합니다.")
    @GetMapping
    public Page<BoardResponseDto> getBoards(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String keyword,   // 없으면 전체조회
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return boardService.getBoards(type, keyword, pageable);
    }

    //게시글 상제조회
    @Operation(summary = "게시글 상세 조회", description = "게시글 ID(boardId)를 통해 특정 게시글의 상세 정보를 조회합니다.")
    @GetMapping("/{boardId}")
    public BoardResponseDto getBoard(
            @PathVariable Long boardId
    ) {
        return boardService.getBoard(boardId);
    }

    //게시글 등록
    @Operation(summary = "게시글 등록",
            description = "새로운 게시글을 작성합니다. 작성자(userId) 정보가 필요합니다.",
            security = @SecurityRequirement(name = "BearerAuth"))
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BoardResponseDto createBoard(
            @RequestBody BoardRequestDto request,
            @RequestParam Long userId
    ) {
        return boardService.createBoard(request, userId);
    }

    //게시글 수정
    @Operation(summary = "게시글 수정",
            description = "기존 게시글을 수정합니다. 작성자 검증을 위해 userId를 수신합니다.",
            security = @SecurityRequirement(name = "BearerAuth"))
    @PutMapping("/{boardId}")
    public BoardResponseDto updateBoard(
            @RequestBody BoardRequestDto request,
            @PathVariable Long boardId,
            @RequestParam Long userId
    ){
        return boardService.updateBoard(boardId, request, userId);
    }

    //게시글 삭제
    @Operation(summary = "게시글 삭제",
            description = "특정 게시글을 삭제합니다. 작성자 검증을 위해 userId를 수신합니다.",
            security = @SecurityRequirement(name = "BearerAuth"))
    @DeleteMapping("/{boardId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBoard(
            @PathVariable Long boardId,
            @RequestParam Long userId
    ){
        boardService.deleteBoard(boardId,userId);
    }
}
