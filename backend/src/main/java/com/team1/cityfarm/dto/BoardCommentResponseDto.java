package com.team1.cityfarm.dto;

import java.time.LocalDateTime;

public class BoardCommentResponseDto {
    private Long id;
    private Long boardId;
    private Long userId;
    private String content;
    private LocalDateTime createdAt;

    public BoardCommentResponseDto(BoardComment boardComment) {
        this.id = boardComment.getId();
        this.boardId = boardComment.getBoardId();
        this.userId = boardComment.getUserId();
        this.content = boardComment.getContent();
        this.createdAt = boardComment.getCreatedAt();
    }
}
