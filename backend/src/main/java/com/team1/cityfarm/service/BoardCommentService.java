package com.team1.cityfarm.service;

import com.team1.cityfarm.dto.BoardCommentRequestDto;
import com.team1.cityfarm.dto.BoardCommentResponseDto;
import com.team1.cityfarm.entity.BoardComment;
import com.team1.cityfarm.repository.BoardCommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardCommentService {

    private final BoardCommentRepository boardCommentRepository;

    //    댓글 등록
    @Transactional
    public BoardCommentResponseDto createComment(Long boardId, BoardCommentRequestDto request, Long userId) {
        BoardComment comment = new BoardComment();
        comment.setContent(request.getContent());
        // comment.setBoard(board);  // TODO
        // comment.setUser(user);    // TODO

        BoardComment saved = boardCommentRepository.save(comment);
        return new BoardCommentResponseDto(saved);
    }

    //    댓글 목록 조회
    

//    댓글 수정

//    댓글 삭제

}
