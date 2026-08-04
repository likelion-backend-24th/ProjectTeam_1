package com.team1.cityfarm.dto;

import lombok.Getter;

@Getter
public class ReplyResponseDto {
    private Long id;
    private Long boardId;
    private Long userId;
    private String content;
    private boolean adopted;

    public ReplyResponseDto(Reply reply) {
        this.id = reply.getId();
        this.boardId = reply.getBoardId();
        this.userId = reply.getUserId();
        this.content = reply.getContent();
        this.adopted = reply.getAdopted();
    }
}
