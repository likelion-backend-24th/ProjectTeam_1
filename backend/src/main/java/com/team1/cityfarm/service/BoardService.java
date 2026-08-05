package com.team1.cityfarm.service;

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

    public Page<Board> getBoards(String type, String keyword, Pageable pageable) {
        if (keyword == null || keyword.isBlank()) {
            return boardRepository.findAll(pageable);
        }

        return switch (type) {
            case "title" -> boardRepository.findByTitleContaining(keyword, pageable);
            case "content" -> boardRepository.findByContentContaining(keyword, pageable);
            case "author" -> boardRepository.findByUser_Nickname(keyword, pageable);
            default -> throw new IllegalArgumentException("잘못된 검색 타입입니다.");
        };

    }
}
