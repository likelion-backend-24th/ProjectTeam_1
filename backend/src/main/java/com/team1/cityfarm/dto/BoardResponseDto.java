package com.team1.cityfarm.dto;

import com.team1.cityfarm.entity.Board;
import com.team1.cityfarm.entity.Category;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "게시글 응답 DTO")
public record BoardResponseDto(

        @Schema(description = "게시글 ID", example = "1")
        Long id,

        @Schema(description = "게시글 제목", example = "오늘 시티팜에 상추 심었어요!")
        String title,

        @Schema(description = "게시글 본문 내용", example = "씨앗부터 키우기 시작했는데 팁 공유해주세요.")
        String content,

        @Schema(description = "작성자 닉네임", example = "파머123")
        String writer,

        @Schema(description = "게시글 카테고리", example = "FREE")
        Category category,

        @Schema(description = "조회수", example = "42")
        int viewCount,

        @Schema(description = "좋아요 수", example = "12")
        int likeCount,

        @Schema(description = "생성 일시", example = "2026-08-05T15:30:00")
        String createdAt,

        @Schema(description = "수정 일시", example = "2026-08-05T15:30:00")
        String updatedAt
) {
    public static BoardResponseDto from(Board board) {
        return new BoardResponseDto(
                board.getId(),
                board.getTitle(),
                board.getContent(),
                board.getUser().getNickname(),
                board.getCategory(),
                board.getViewCount(),
                board.getLikeCount(),
                board.getCreatedAt().toString(),
                board.getUpdatedAt().toString()
        );
    }
}