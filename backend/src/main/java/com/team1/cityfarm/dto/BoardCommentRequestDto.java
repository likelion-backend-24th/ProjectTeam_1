package com.team1.cityfarm.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class BoardCommentRequestDto {

    @NotBlank(message = "댓글 내용을 입력해주세요")
    private String content;

}
