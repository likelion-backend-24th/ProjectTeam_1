package com.team1.cityfarm.dto;

import com.team1.cityfarm.entity.Board;
import com.team1.cityfarm.entity.Reply;
import com.team1.cityfarm.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter@AllArgsConstructor@NoArgsConstructor
public class ReplyResponseDto {
    private Long id;
    private String content;
    private Boolean isAdopted;
    private Board board;
    private User user;

    public static ReplyResponseDto from(Reply reply){
        return new ReplyResponseDto(
                reply.getId(),
                reply.getContent(),
                reply.getIsAdopted(),
                reply.getBoard(),
                reply.getUser()
        );
    }
}
