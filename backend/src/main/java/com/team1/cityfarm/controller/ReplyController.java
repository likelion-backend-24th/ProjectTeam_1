package com.team1.cityfarm.controller;

import com.team1.cityfarm.dto.ReplyCreateRequestDto;
import com.team1.cityfarm.dto.ReplyResponseDto;
import com.team1.cityfarm.entity.Reply;
import com.team1.cityfarm.service.ReplyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReplyController {
    private final ReplyService replyService;

    @GetMapping("/board/{id}/reply")
    public ResponseEntity<List<Reply>> getReply(@PathVariable("id") Long boardId
                                                ){
        return ResponseEntity.ok(null);
    }

    @PostMapping("/board/{id}/reply")
    public ResponseEntity<ReplyResponseDto> createReply(@PathVariable("id") Long boardId,
                                                        @Valid ReplyCreateRequestDto dto){
        return ResponseEntity.ok(replyService.createReply(null,boardId,dto));
    }
}
