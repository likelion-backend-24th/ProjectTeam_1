package com.team1.cityfarm.service;

import com.team1.cityfarm.dto.BoardRequestDto;
import com.team1.cityfarm.dto.BoardResponseDto;
import com.team1.cityfarm.entity.Board;
import com.team1.cityfarm.entity.Category;
import com.team1.cityfarm.entity.RoleType;
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
    public Page<BoardResponseDto> getBoards(String type, String keyword, Category category, Pageable pageable) {
        boolean hasKeyword = keyword != null && !keyword.isBlank();

        Page<Board> boards;

        if (category == null && !hasKeyword) {
            boards = boardRepository.findAll(pageable);
        } else if (category != null && !hasKeyword) {
            boards = boardRepository.findByCategory(category, pageable);
        } else if (category == null) {
            boards = switch (type) {
                case "title" -> boardRepository.findByTitleContaining(keyword, pageable);
                case "content" -> boardRepository.findByContentContaining(keyword, pageable);
                case "author" -> boardRepository.findByUser_Nickname(keyword, pageable);
                default -> throw new CustomException(CustomError.INVALID_INPUT_VALUE);
            };
        } else {
            boards = switch (type) {
                case "title" -> boardRepository.findByCategoryAndTitleContaining(category, keyword, pageable);
                case "content" -> boardRepository.findByCategoryAndContentContaining(category, keyword, pageable);
                case "author" -> boardRepository.findByCategoryAndUser_Nickname(category, keyword, pageable);
                default -> throw new CustomException(CustomError.INVALID_INPUT_VALUE);
            };
        }

        return boards.map(BoardResponseDto::from);
    }

    //게시글 상세 조회 (조회수 증가)
    @Transactional
    public BoardResponseDto getBoard(Long boardId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new CustomException(CustomError.BOARD_NOT_FOUND));
        board.increaseViewCount();
        return BoardResponseDto.from(board);
    }

    //게시글 등록
    @Transactional
    public BoardResponseDto createBoard(BoardRequestDto request, Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(CustomError.USER_NOT_FOUND));

        // 공지사항 권한 검증 - 401 → 403으로 수정 (이미 로그인된 사용자의 권한 부족 상황)
        if (request.category() == Category.NOTICE && user.getRoleType() != RoleType.ADMIN) {
            throw new CustomException(CustomError.AUTH_FORBIDDEN);
        }

        Board board = Board.builder()
                .title(request.title())
                .content(request.content())
                .category(request.category())
                .user(user)
                .build();
        return BoardResponseDto.from(boardRepository.save(board));
    }

    // 게시글 수정
    @Transactional
    public BoardResponseDto updateBoard(Long boardId, BoardRequestDto request, Long userId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new CustomException(CustomError.BOARD_NOT_FOUND));
        if (!board.getUser().getId().equals(userId)) {
            throw new CustomException(CustomError.BOARD_NOT_OWNER);
        }

        // 요청자의 role을 직접 조회해서 검증 - 원작성자 role이 아니라 실제 요청자 role을 봐야 함
        User requester = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(CustomError.USER_NOT_FOUND));

        if (request.category() == Category.NOTICE && requester.getRoleType() != RoleType.ADMIN) {
            throw new CustomException(CustomError.AUTH_FORBIDDEN);
        }

        board.update(request.title(), request.content(), request.category());
        return BoardResponseDto.from(boardRepository.save(board));
    }

    //게시글 삭제
    @Transactional
    public void deleteBoard(Long boardId,Long userId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new CustomException(CustomError.BOARD_NOT_FOUND));
        if(!board.getUser().getId().equals(userId)){
            throw new CustomException(CustomError.BOARD_NOT_OWNER);
        }
        boardRepository.delete(board);
    }




}
