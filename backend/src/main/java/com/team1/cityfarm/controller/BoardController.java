package com.team1.cityfarm.controller;

import com.team1.cityfarm.dto.BoardRequestDto;
import com.team1.cityfarm.dto.BoardResponseDto;
import com.team1.cityfarm.entity.Category;
import com.team1.cityfarm.global.response.ApiResponse;
import com.team1.cityfarm.global.security.user.CustomUserDetails;
import com.team1.cityfarm.service.BoardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "게시판 API", description = "게시글 조회, 등록, 수정, 삭제 API")
@RestController
@RequestMapping("/api/board")
@RequiredArgsConstructor
public class BoardController {
    private final BoardService boardService;

    // 게시글 목록조회
    @Operation(summary = "게시글 목록 조회",
            description = "type과 keyword를 조건으로 게시글 목록을 조회합니다.(기본-전체조회)")
    @GetMapping
    public ApiResponse<Page<BoardResponseDto>> getBoards(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String keyword,   // 없으면 전체조회
            @RequestParam(required = false) Category category, // 없으면 전체 카테고리(공지/질문/자유 탭)
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ApiResponse.success("게시글 목록 조회 성공", boardService.getBoards(type, keyword, category, pageable));
    }

    //게시글 상세 조회
    @Operation(summary = "게시글 상세 조회",
            description = "게시글 ID(boardId)를 통해 특정 게시글의 상세 정보를 조회합니다.")
    @GetMapping("/{boardId}")
    public ApiResponse<BoardResponseDto> getBoard(@PathVariable Long boardId) {
        return ApiResponse.success("게시글 조회 성공", boardService.getBoard(boardId));
    }

    // 게시글 등록
    @Operation(summary = "게시글 등록",
            description = "게시글을 등록합니다. 이미지는 최대 5장까지 첨부할 수 있습니다.",
            security = @SecurityRequirement(name = "BearerAuth"))
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<BoardResponseDto> createBoard(
            @RequestPart("request") @Valid BoardRequestDto request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getUserId();
        return ApiResponse.success("게시글 등록 성공", boardService.createBoard(request, images, userId));
    }

    // 게시글 수정
    @Operation(summary = "게시글 수정",
            description = "업데이트 내역을 받아 게시글을 수정합니다. 이미지를 새로 보내면 기존 이미지를 전체 교체하고, 보내지 않으면 기존 이미지를 유지합니다.",
            security = @SecurityRequirement(name = "BearerAuth"))
    @PutMapping(value = "/{boardId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<BoardResponseDto> updateBoard(
            @PathVariable Long boardId,
            @RequestPart("request") @Valid BoardRequestDto request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getUserId();
        return ApiResponse.success("게시글 수정 성공", boardService.updateBoard(boardId, request, images, userId));
    }

    // 게시글 삭제
    @Operation(summary = "게시글 삭제",
            description = "게시글 ID(boardId)를 받아 해당 게시글을 삭제합니다.",
            security = @SecurityRequirement(name = "BearerAuth"))
    @DeleteMapping("/{boardId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBoard(
            @PathVariable Long boardId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getUserId();
        boardService.deleteBoard(boardId, userId);
    }
}