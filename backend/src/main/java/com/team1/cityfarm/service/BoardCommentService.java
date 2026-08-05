package com.team1.cityfarm.service;

import com.team1.cityfarm.dto.BoardCommentRequestDto;
import com.team1.cityfarm.dto.BoardCommentResponseDto;
import com.team1.cityfarm.entity.BoardComment;
import com.team1.cityfarm.repository.BoardCommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardCommentService {

    private final BoardCommentRepository boardCommentRepository;

    // 댓글 등록
    @Transactional
    public BoardCommentResponseDto createComment(Long boardId, BoardCommentRequestDto request, Long userId) {
        BoardComment comment = new BoardComment();
        comment.setContent(request.getContent());
        // comment.setBoard(board);  // TODO
        // comment.setUser(user);    // TODO

        BoardComment saved = boardCommentRepository.save(comment);
        return new BoardCommentResponseDto(saved);
    }

    // 댓글 목록 조회
    public List<BoardCommentResponseDto> getComments(Long boardId) {

        // TODO: Board 연관관계 살아나면 아래 주석 해제하고 이 return 지우기
        /*List<BoardComment> comments = boardCommentRepository.findByBoardIdOrderByCreatedAtAsc(boardId);
        return comments.stream()
                .map(comment -> new BoardCommentResponseDto(comment))
                .collect(Collectors.toList());*/

        return Collections.emptyList();
    }

    // 댓글 수정
    @Transactional
    public BoardCommentResponseDto updateComment(Long commentId, BoardCommentRequestDto requestDto, Long userId) {
        BoardComment comment = boardCommentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 댓글입니다."));

        // TODO: 작성자 본인 확인 로직 (user 연관관계 살아나면)
        /*if (!comment.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("본인 댓글만 수정할 수 있습니다.");
        }*/

        comment.setContent(requestDto.getContent());
        BoardComment updated = boardCommentRepository.saveAndFlush(comment);
        return new BoardCommentResponseDto(updated);
    }

    // 댓글 삭제
    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        BoardComment comment = boardCommentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 댓글입니다."));

        // TODO: 작성자 본인 확인 로직 (user 연관관계 살아나면)
        /*if (!comment.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("본인 댓글만 삭제할 수 있습니다.");
        }*/

        boardCommentRepository.delete(comment);
    }
}