package com.team1.cityfarm.controller;

import com.team1.cityfarm.dto.ReplyCreateRequestDto;
import com.team1.cityfarm.dto.ReplyResponseDto;
import com.team1.cityfarm.entity.Reply;
import com.team1.cityfarm.service.ReplyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReplyController {
    private final ReplyService replyService;

    //상세 게시판에서 답글 불러오기
    @GetMapping("/board/{id}/reply")
    public ResponseEntity<List<ReplyResponseDto>> getReply(@PathVariable("id") Long boardId
                                                ){
        return ResponseEntity.ok(replyService.getAllReply(boardId));
    }

    //상세 게시판에서 답글 작성
    @PostMapping("/board/{id}/reply")
    public ResponseEntity<ReplyResponseDto> createReply(@PathVariable("id") Long boardId,
                                                        @Valid ReplyCreateRequestDto dto){
        //userDetails 추가되면 구현 예정

        return ResponseEntity.ok(replyService.createReply(null,boardId,dto));
    }

    @PatchMapping("/reply/{id}")
    public ResponseEntity<ReplyResponseDto> editReply(@PathVariable("id") Long replyId,
                                                      @Valid ReplyCreateRequestDto dto){
        //userDetails 추가되면 구현 예정
        return ResponseEntity.ok(replyService.editReply(null,replyId,dto));
    }
}
