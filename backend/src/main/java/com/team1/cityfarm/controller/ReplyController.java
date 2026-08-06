package com.team1.cityfarm.controller;

import com.team1.cityfarm.dto.ReplyCreateRequestDto;
import com.team1.cityfarm.dto.ReplyResponseDto;
import com.team1.cityfarm.global.security.CustomUserDetails;
import com.team1.cityfarm.service.ReplyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "답글 API", description = "답글 조회 및 작성 API")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReplyController {
    private final ReplyService replyService;

    //상세 게시판에서 답글 불러오기
    @Operation(summary = "상세 게시판에서 답글 조회",
            description = "상세 게시판에 달린 답글을 불러옵니다.")
    @GetMapping("/board/{id}/reply")
    public ResponseEntity<List<ReplyResponseDto>> getReply(@PathVariable("id") Long boardId
                                                ){
        return ResponseEntity.ok(replyService.getAllReply(boardId));
    }

    //상세 게시판에서 답글 작성
    @Operation(summary = "상세 게시판에서 답글 작성",
            description = "상세 게시판에 답글을 등록합니다.",
            security = @SecurityRequirement(name = "BearerAuth"))
    @PostMapping("/board/{id}/reply")
    public ResponseEntity<ReplyResponseDto> createReply(@PathVariable("id") Long boardId,
                                                        @Valid ReplyCreateRequestDto dto,
                                                        @AuthenticationPrincipal CustomUserDetails customUserDetails){
        return ResponseEntity.ok(replyService.createReply(customUserDetails.getUser().getId(), boardId,dto));
    }

    @Operation(summary = "답글 수정",
            description = "답글의 내용을 수정합니다.",
            security = @SecurityRequirement(name = "BearerAuth"))
    @PatchMapping("/reply/{id}")
    public ResponseEntity<ReplyResponseDto> editReply(@PathVariable("id") Long replyId,
                                                      @Valid ReplyCreateRequestDto dto,
                                                      @AuthenticationPrincipal CustomUserDetails customUserDetails){
        return ResponseEntity.ok(replyService.editReply(customUserDetails.getUser().getId(),replyId,dto));
    }

    @Operation(summary = "답글 삭제",
            description = "답글을 삭제합니다.",
            security = @SecurityRequirement(name = "BearerAuth"))
    @DeleteMapping("/reply/{id}")
    public ResponseEntity<String> deleteReply(@PathVariable("id") Long replyId,
                                         @AuthenticationPrincipal CustomUserDetails customUserDetails){
        replyService.deleteReply(customUserDetails.getUser().getId(), replyId);
        return ResponseEntity.ok("삭제 성공");
    }
}
