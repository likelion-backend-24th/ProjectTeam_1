package com.team1.cityfarm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "답글 작성 요청 DTO")
public class ReplyCreateRequestDto {

    @Schema(description = "답글 내용", example = "상추는 거꾸로 해도 상추랍니다.")
    @NotBlank(message = "내용을 입력해주세요")
    private String content;
}