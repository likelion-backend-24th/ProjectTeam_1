package com.team1.cityfarm.dto;

import com.team1.cityfarm.entity.ReplyComment;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Schema(description = "답글-댓글 응답 DTO")
public class ReplyCommentResponseDto {

    @Schema(description = "답글-댓글 ID", example = "1")
    private final Long id;

    @Schema(description = "답글-댓글 내용", example = "헐 몰랐어요")
    private final String content;

    @Schema(description = "작성자 닉네임", example = "파머123")
    private final String nickname;

    @Schema(description = "생성 일시", example = "2026-08-05T15:30:00")
    private final LocalDateTime createdAt;

    @Schema(description = "수정 일시", example = "2026-08-05T15:30:00")
    private final LocalDateTime updatedAt;

    public ReplyCommentResponseDto(ReplyComment replyComment) {
        this.id = replyComment.getId();
        this.content = replyComment.getContent();
        this.nickname = replyComment.getUser().getNickname();
        this.createdAt = replyComment.getCreatedAt();
        this.updatedAt = replyComment.getUpdatedAt();
    }
}