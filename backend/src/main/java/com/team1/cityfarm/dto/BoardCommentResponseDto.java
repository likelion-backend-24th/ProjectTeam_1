package com.team1.cityfarm.dto;

import com.team1.cityfarm.entity.BoardComment;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class BoardCommentResponseDto {

    private final Long id;
    private final String content;
//  private final String nickname; // TODO: User 연관관계 살아나면 추가
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public BoardCommentResponseDto(BoardComment boardComment) {
        this.id = boardComment.getId();
        this.content = boardComment.getContent();
//      this.nickname = boardComment.getUser().getNickname(); // TODO : 연관관계 살아나면 추가
        this.createdAt = boardComment.getCreatedAt();
        this.updatedAt = boardComment.getUpdatedAt();
    }

}
