package com.team1.cityfarm.service;

import com.team1.cityfarm.dto.BoardRequestDto;
import com.team1.cityfarm.dto.BoardResponseDto;
import com.team1.cityfarm.entity.Board;
import com.team1.cityfarm.entity.BoardImage;
import com.team1.cityfarm.entity.Category;
import com.team1.cityfarm.entity.RoleType;
import com.team1.cityfarm.entity.User;
import com.team1.cityfarm.global.exception.CustomError;
import com.team1.cityfarm.global.exception.CustomException;
import com.team1.cityfarm.repository.BoardImageRepository;
import com.team1.cityfarm.repository.BoardRepository;
import com.team1.cityfarm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BoardService {

    private static final int BOARD_IMAGE_MAX_COUNT = 5;

    private final BoardRepository boardRepository;
    private final UserRepository userRepository;
    private final BoardImageRepository boardImageRepository;
    private final FileStorageService fileStorageService;

    // 게시글 목록 조회
    @Transactional(readOnly = true)
    public Page<BoardResponseDto> getBoards(String type, String keyword, Category category, Pageable pageable) {
        boolean hasKeyword = keyword != null && !keyword.isBlank();

        Page<Board> boards;

        if (category == null && !hasKeyword) {
            boards = boardRepository.findAll(pageable);
        } else if (category != null && !hasKeyword) {
            boards = boardRepository.findByCategory(category, pageable);
        } else if (category == null) {
            String searchType = (type != null) ? type : "title";
            boards = switch (searchType) {
                case "title" -> boardRepository.findByTitleContaining(keyword, pageable);
                case "content" -> boardRepository.findByContentContaining(keyword, pageable);
                case "author" -> boardRepository.findByUser_Nickname(keyword, pageable);
                default -> throw new CustomException(CustomError.INVALID_INPUT_VALUE);
            };
        } else {
            String searchType = (type != null) ? type : "title";
            boards = switch (searchType) {
                case "title" -> boardRepository.findByCategoryAndTitleContaining(category, keyword, pageable);
                case "content" -> boardRepository.findByCategoryAndContentContaining(category, keyword, pageable);
                case "author" -> boardRepository.findByCategoryAndUser_Nickname(category, keyword, pageable);
                default -> throw new CustomException(CustomError.INVALID_INPUT_VALUE);
            };
        }

        List<Long> boardIds = boards.map(Board::getId).getContent();
        Map<Long, List<String>> imageUrlsByBoardId = boardIds.isEmpty()
                ? Map.of()
                : boardImageRepository.findByBoard_IdInOrderByIdAsc(boardIds).stream()
                        .collect(Collectors.groupingBy(
                                image -> image.getBoard().getId(),
                                Collectors.mapping(BoardImage::getImageUrl, Collectors.toList())
                        ));

        return boards.map(board -> BoardResponseDto.from(
                board,
                imageUrlsByBoardId.getOrDefault(board.getId(), List.of())
        ));
    }

    //게시글 상세 조회 (조회수 증가)
    @Transactional
    public BoardResponseDto getBoard(Long boardId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new CustomException(CustomError.BOARD_NOT_FOUND));
        board.increaseViewCount();

        List<String> imageUrls = boardImageRepository.findByBoard_IdOrderByIdAsc(boardId).stream()
                .map(BoardImage::getImageUrl)
                .toList();

        return BoardResponseDto.from(board, imageUrls);
    }

    //게시글 등록
    @Transactional
    public BoardResponseDto createBoard(BoardRequestDto request, List<MultipartFile> images, Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(CustomError.USER_NOT_FOUND));

        // 공지사항 권한 검증 - 401 → 403으로 수정 (이미 로그인된 사용자의 권한 부족 상황)
        if (request.category() == Category.NOTICE && user.getRoleType() != RoleType.ADMIN) {
            throw new CustomException(CustomError.AUTH_FORBIDDEN);
        }

        if (images != null && images.size() > BOARD_IMAGE_MAX_COUNT) {
            throw new CustomException(CustomError.BOARD_IMAGE_LIMIT);
        }

        Board board = Board.builder()
                .title(request.title())
                .content(request.content())
                .category(request.category())
                .user(user)
                .build();
        Board savedBoard = boardRepository.save(board);

        List<String> imageUrls = storeBoardImages(savedBoard, images);

        return BoardResponseDto.from(savedBoard, imageUrls);
    }

    // 게시글 수정
    @Transactional
    public BoardResponseDto updateBoard(Long boardId, BoardRequestDto request, List<MultipartFile> images, Long userId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new CustomException(CustomError.BOARD_NOT_FOUND));

        // 요청자의 role을 직접 조회해서 검증 - 원작성자 role이 아니라 실제 요청자 role을 봐야 함
        User requester = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(CustomError.USER_NOT_FOUND));

        boolean isOwner = board.getUser().getId().equals(userId);
        boolean isAdmin = requester.getRoleType() == RoleType.ADMIN;
        if (!isOwner && !isAdmin) {
            throw new CustomException(CustomError.BOARD_NOT_OWNER);
        }

        if (request.category() == Category.NOTICE && !isAdmin) {
            throw new CustomException(CustomError.AUTH_FORBIDDEN);
        }

        if (images != null && images.size() > BOARD_IMAGE_MAX_COUNT) {
            throw new CustomException(CustomError.BOARD_IMAGE_LIMIT);
        }

        board.update(request.title(), request.content(), request.category());
        Board savedBoard = boardRepository.save(board);

        // 새 이미지가 들어온 경우에만 기존 이미지를 전체 교체 (안 보내면 기존 이미지 유지)
        List<String> imageUrls;
        if (images != null && !images.isEmpty()) {
            boardImageRepository.deleteAll(boardImageRepository.findByBoard_IdOrderByIdAsc(boardId));
            imageUrls = storeBoardImages(savedBoard, images);
        } else {
            imageUrls = boardImageRepository.findByBoard_IdOrderByIdAsc(boardId).stream()
                    .map(BoardImage::getImageUrl)
                    .toList();
        }

        return BoardResponseDto.from(savedBoard, imageUrls);
    }

    //게시글 삭제
    @Transactional
    public void deleteBoard(Long boardId, Long userId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new CustomException(CustomError.BOARD_NOT_FOUND));

        User requester = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(CustomError.USER_NOT_FOUND));

        boolean isOwner = board.getUser().getId().equals(userId);
        boolean isAdmin = requester.getRoleType() == RoleType.ADMIN;
        if (!isOwner && !isAdmin) {
            throw new CustomException(CustomError.BOARD_NOT_OWNER);
        }

        boardImageRepository.deleteAll(boardImageRepository.findByBoard_IdOrderByIdAsc(boardId));
        boardRepository.delete(board);
    }

    private List<String> storeBoardImages(Board board, List<MultipartFile> images) {
        if (images == null || images.isEmpty()) {
            return List.of();
        }

        List<String> imageUrls = new ArrayList<>();
        for (MultipartFile image : images) {
            String imageUrl = fileStorageService.store(image, "boards");
            boardImageRepository.save(BoardImage.builder()
                    .board(board)
                    .imageUrl(imageUrl)
                    .build());
            imageUrls.add(imageUrl);
        }
        return imageUrls;
    }


}
