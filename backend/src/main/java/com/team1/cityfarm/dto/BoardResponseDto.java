package com.team1.cityfarm.dto;

import lombok.Getter;
import java.time.LocalDateTime;

@Getter
public class BoardResponseDto {
    private Long id;
    private Long userId;
    private String title;
    private String content;
    private String category;
    private int viewCount;
    private int likeCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Entity -> DTO 변환 생성자
    public BoardResponseDto(Board board) {
        this.id = board.getId();
        this.userId = board.getUserId();
        this.title = board.getTitle();
        this.content = board.getContent();
        this.category = board.getCategory();
        this.viewCount = board.getViewCount();
        this.likeCount = board.getLikeCount();
        this.createdAt = board.getCreatedAt();
        this.updatedAt = board.getUpdatedAt();
    }
}