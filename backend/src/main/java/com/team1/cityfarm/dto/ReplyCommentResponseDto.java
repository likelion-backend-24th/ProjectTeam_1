package com.team1.cityfarm.dto;

import com.team1.cityfarm.entity.ReplyComment;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ReplyCommentResponseDto {

    private final Long id;
    private final String content;
    //  private final String nickname; // TODO: User 연관관계 살아나면 추가
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public ReplyCommentResponseDto(ReplyComment replyComment) {
        this.id = replyComment.getId();
        this.content = replyComment.getContent();
//      this.nickname = replyComment.getUser().getNickname(); // TODO
        this.createdAt = replyComment.getCreatedAt();
        this.updatedAt = replyComment.getUpdatedAt();
    }
}
