package com.team1.cityfarm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "게시글-댓글 작성 요청 DTO")
public class BoardCommentRequestDto {

    @Schema(description = "게시판 댓글", example = "상추 먹고싶네요..")
    @NotBlank(message = "댓글 내용을 입력해주세요")
    private String content;

}
