package com.team1.cityfarm.dto;


import java.time.LocalDateTime;

public class ReplyCommentResponseDto {
    private Long id;
    private Long replyId;
    private Long userId;
    private String content;
    private LocalDateTime createdAt;

    public ReplyCommentResponseDto(ReplyComment replyComment) {
        this.id = replyComment.getId();
        this.replyId = replyComment.getReplyId();
        this.userId = replyComment.getUserId();
        this.content = replyComment.getContent();
        this.createdAt = replyComment.getCreatedAt();
    }
}
