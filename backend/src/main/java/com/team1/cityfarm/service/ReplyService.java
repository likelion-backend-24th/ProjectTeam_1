package com.team1.cityfarm.service;

import com.team1.cityfarm.dto.ReplyCreateRequestDto;
import com.team1.cityfarm.dto.ReplyResponseDto;
import com.team1.cityfarm.entity.Reply;
import com.team1.cityfarm.entity.User;
import com.team1.cityfarm.global.exception.CustomError;
import com.team1.cityfarm.global.exception.CustomException;
import com.team1.cityfarm.repository.ReplyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service@RequiredArgsConstructor
public class ReplyService {
    private final ReplyRepository replyRepository;

    //답글 작성
    @Transactional
    public ReplyResponseDto createReply(User user, Long boardId, ReplyCreateRequestDto dto){
        
        return null;
    }

    //답글 수정
    @Transactional
    public ReplyResponseDto editReply(User user, Long id, ReplyCreateRequestDto dto){
        Reply loadReply = replyRepository.findById(id).orElseThrow(()->new CustomException(CustomError.REPLY_NOT_FOUND));
        loadReply.setContent(dto.getContent());
        replyRepository.save(loadReply);
        return ReplyResponseDto.from(loadReply);
    }
}
