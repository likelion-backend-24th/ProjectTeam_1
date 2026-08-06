package com.team1.cityfarm.controller;

import com.team1.cityfarm.dto.ReplyCreateRequestDto;
import com.team1.cityfarm.dto.ReplyResponseDto;
import com.team1.cityfarm.service.ReplyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
            description = "상세 게시판에 달린 답글을 불러옵니다."
    )
    @GetMapping("/board/{id}/reply")
    public ResponseEntity<List<ReplyResponseDto>> getReply(@PathVariable("id") Long boardId
                                                ){
        return ResponseEntity.ok(replyService.getAllReply(boardId));
    }

    //상세 게시판에서 답글 작성
    @Operation(summary = "상세 게시판에서 답글 작성",
            description = "상세 게시판에 답글을 등록합니다",
            security = @SecurityRequirement(name = "BearerAuth") // Swagger 상단 자물쇠/토큰 표시
    )
    @PostMapping("/board/{id}/reply")
    public ResponseEntity<ReplyResponseDto> createReply(@PathVariable("id") Long boardId,
                                                        @Valid ReplyCreateRequestDto dto){
        //userDetails 추가되면 구현 예정

        return ResponseEntity.ok(replyService.createReply(null,boardId,dto));
    }

    @Operation(summary = "답글 수정",
            description = "답글의 내용을 수정합니다"
    )
    @PatchMapping("/reply/{id}")
    public ResponseEntity<ReplyResponseDto> editReply(@PathVariable("id") Long replyId,
                                                      @Valid ReplyCreateRequestDto dto){
        //userDetails 추가되면 구현 예정
        return ResponseEntity.ok(replyService.editReply(null,replyId,dto));
    }
}
