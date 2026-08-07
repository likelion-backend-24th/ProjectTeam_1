package com.team1.cityfarm.service;

import com.team1.cityfarm.dto.BoardLikeResponseDto;
import com.team1.cityfarm.entity.Board;
import com.team1.cityfarm.entity.BoardLike;
import com.team1.cityfarm.entity.User;
import com.team1.cityfarm.global.exception.CustomError;
import com.team1.cityfarm.global.exception.CustomException;
import com.team1.cityfarm.repository.BoardLikeRepository;
import com.team1.cityfarm.repository.BoardRepository;
import com.team1.cityfarm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BoardLikeService {

    private final BoardLikeRepository boardLikeRepository;
    private final BoardRepository boardRepository;
    private final UserRepository userRepository;

    // 게시글 좋아요 등록 (이미 좋아요한 경우 그대로 유지)
    @Transactional
    public BoardLikeResponseDto likeBoard(Long boardId, Long userId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new CustomException(CustomError.BOARD_NOT_FOUND));

        if (!boardLikeRepository.existsByBoard_IdAndUser_Id(boardId, userId)) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new CustomException(CustomError.USER_NOT_FOUND));

            boardLikeRepository.save(BoardLike.builder()
                    .board(board)
                    .user(user)
                    .build());
            board.increaseLikeCount();
        }

        return new BoardLikeResponseDto(true, board.getLikeCount());
    }

    // 게시글 좋아요 취소 (좋아요한 적 없는 경우 그대로 유지)
    @Transactional
    public BoardLikeResponseDto unlikeBoard(Long boardId, Long userId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new CustomException(CustomError.BOARD_NOT_FOUND));

        boardLikeRepository.findByBoard_IdAndUser_Id(boardId, userId)
                .ifPresent(boardLike -> {
                    boardLikeRepository.delete(boardLike);
                    board.decreaseLikeCount();
                });

        return new BoardLikeResponseDto(false, board.getLikeCount());
    }
}
