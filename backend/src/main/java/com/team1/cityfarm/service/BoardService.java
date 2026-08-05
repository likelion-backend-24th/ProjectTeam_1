package com.team1.cityfarm.service;

import com.team1.cityfarm.dto.BoardRequestDto;
import com.team1.cityfarm.dto.BoardResponseDto;
import com.team1.cityfarm.entity.Board;
import com.team1.cityfarm.entity.User;
import com.team1.cityfarm.global.exception.CustomError;
import com.team1.cityfarm.global.exception.CustomException;
import com.team1.cityfarm.repository.BoardRepository;
import com.team1.cityfarm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BoardService {

    private final BoardRepository boardRepository;
    private final UserRepository userRepository;

    //게시글 목록 조회
    @Transactional(readOnly = true)
    public Page<BoardResponseDto> getBoards(String type, String keyword, Pageable pageable) {
        if (keyword == null || keyword.isBlank()) {
            return boardRepository.findAll(pageable).map(BoardResponseDto::from);
        }

        Page<Board> boards = switch (type) {
            case "title" -> boardRepository.findByTitleContaining(keyword, pageable);
            case "content" -> boardRepository.findByContentContaining(keyword, pageable);
            case "author" -> boardRepository.findByUser_Nickname(keyword, pageable);
            default -> throw new CustomException(CustomError.INVALID_INPUT_VALUE);
        };

        return boards.map(BoardResponseDto::from);   // 여기서 DTO 변환하고 리턴
    }

    //게시글 상세 조회
    @Transactional(readOnly = true)
    public BoardResponseDto getBoard(Long boardId) {
        return boardRepository.findById(boardId).map(BoardResponseDto::from).orElseThrow();
    }




}
