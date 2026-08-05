package com.team1.cityfarm.dto;

import com.team1.cityfarm.entity.BoardComment;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Schema(description = "게시글-댓글 응답 DTO")
public class BoardCommentResponseDto {

    @Schema(description = "게시글-댓글 id", example = "1")
    private final Long id;

    @Schema(description = "댓글 내용", example = "상추 먹고싶네요..")
    private final String content;

    //  private final String nickname; // TODO: User 연관관계 살아나면 추가

    @Schema(description = "작성일시", example = "2026-08-05T15:30:00")
    private final LocalDateTime createdAt;

    @Schema(description = "수정일시", example = "2026-08-05T15:30:00")
    private final LocalDateTime updatedAt;

    public BoardCommentResponseDto(BoardComment boardComment) {
        this.id = boardComment.getId();
        this.content = boardComment.getContent();
//      this.nickname = boardComment.getUser().getNickname(); // TODO
        this.createdAt = boardComment.getCreatedAt();
        this.updatedAt = boardComment.getUpdatedAt();
    }
}