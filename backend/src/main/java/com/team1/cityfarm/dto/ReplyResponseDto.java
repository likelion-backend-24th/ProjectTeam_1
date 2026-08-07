package com.team1.cityfarm.dto;

import com.team1.cityfarm.entity.Board;
import com.team1.cityfarm.entity.Reply;
import com.team1.cityfarm.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "답글 응답 DTO")
public class ReplyResponseDto {

    @Schema(description = "답글 ID", example = "1")
    private Long id;

    @Schema(description = "답글 내용", example = "상추는 거꾸로 해도 상추랍니다.")
    private String content;

    @Schema(description = "채택 여부", example = "true")
    private Boolean isAdopted;

    @Schema(description = "게시글 id")
    private Long boardId;

    @Schema(description = "작성자 id")
    private Long userId;

    public static ReplyResponseDto from(Reply reply) {
        return new ReplyResponseDto(
                reply.getId(),
                reply.getContent(),
                reply.getIsAdopted(),
                reply.getBoard().getId(),
                reply.getUser().getId()
        );
    }
}