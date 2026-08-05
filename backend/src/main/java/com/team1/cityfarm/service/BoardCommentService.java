package com.team1.cityfarm.service;

import com.team1.cityfarm.dto.BoardCommentRequestDto;
import com.team1.cityfarm.dto.BoardCommentResponseDto;
import com.team1.cityfarm.entity.Board;
import com.team1.cityfarm.entity.BoardComment;
import com.team1.cityfarm.entity.User;
import com.team1.cityfarm.repository.BoardCommentRepository;
import com.team1.cityfarm.repository.BoardRepository;
import com.team1.cityfarm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardCommentService {

    private final BoardCommentRepository boardCommentRepository;
    private final UserRepository userRepository;
    private final BoardRepository boardRepository;

    // 댓글 등록
    @Transactional
    public BoardCommentResponseDto createComment(Long boardId, BoardCommentRequestDto request, Long userId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        BoardComment comment = new BoardComment();
        comment.setContent(request.getContent());
         comment.setBoard(board);
         comment.setUser(user);

        BoardComment saved = boardCommentRepository.save(comment);
        return new BoardCommentResponseDto(saved);
    }

    // 댓글 목록 조회
    public List<BoardCommentResponseDto> getComments(Long boardId) {

        List<BoardComment> comments = boardCommentRepository.findByBoardIdOrderByCreatedAtAsc(boardId);
        return comments.stream()
                .map(comment -> new BoardCommentResponseDto(comment))
                .collect(Collectors.toList());

    }

    // 댓글 수정
    @Transactional
    public BoardCommentResponseDto updateComment(Long commentId, BoardCommentRequestDto requestDto, Long userId) {
        BoardComment comment = boardCommentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 댓글입니다."));

        if (!comment.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("본인 댓글만 수정할 수 있습니다.");
        }

        comment.setContent(requestDto.getContent());
        BoardComment updated = boardCommentRepository.saveAndFlush(comment);
        return new BoardCommentResponseDto(updated);
    }

    // 댓글 삭제
    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        BoardComment comment = boardCommentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 댓글입니다."));

        if (!comment.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("본인 댓글만 삭제할 수 있습니다.");
        }

        boardCommentRepository.delete(comment);
    }
}