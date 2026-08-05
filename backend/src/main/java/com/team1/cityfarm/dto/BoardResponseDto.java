package com.team1.cityfarm.dto;


import com.team1.cityfarm.entity.Board;
import com.team1.cityfarm.entity.Category;

public record BoardResponseDto (
    Long id,
    String title,
    String content,
    String writer,
    Category category,
    int viewCount,
    String createdAt,
    String updatedAt
){
    public static BoardResponseDto from(Board board) {
        return new BoardResponseDto(
                board.getId(),
                board.getTitle(),
                board.getContent(),
                board.getUser().getNickname(),
                board.getCategory(),
                board.getViewCount(),
                board.getCreatedAt().toString(),
                board.getUpdatedAt().toString());
    }
}
