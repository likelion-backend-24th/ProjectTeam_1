package com.team1.cityfarm.service;

import com.team1.cityfarm.dto.BoardResponseDto;
import com.team1.cityfarm.entity.Board;
import com.team1.cityfarm.repository.BoardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BoardService {

    private final BoardRepository boardRepository;

    public Page<BoardResponseDto> getBoards(String type, String keyword, Pageable pageable) {
        if (keyword == null || keyword.isBlank()) {
            return boardRepository.findAll(pageable).map(BoardResponseDto::from);
        }

        Page<Board> boards = switch (type) {
            case "title" -> boardRepository.findByTitleContaining(keyword, pageable);
            case "content" -> boardRepository.findByContentContaining(keyword, pageable);
            case "author" -> boardRepository.findByUser_Nickname(keyword, pageable);
            default -> throw new IllegalArgumentException("잘못된 검색 타입입니다.");
        };

        return boards.map(BoardResponseDto::from);   // 여기서 DTO 변환하고 리턴
    }
}
