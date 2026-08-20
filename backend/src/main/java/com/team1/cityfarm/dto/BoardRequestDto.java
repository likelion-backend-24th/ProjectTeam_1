package com.team1.cityfarm.dto;

import com.team1.cityfarm.entity.Category;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "게시글 작성 및 수정 요청 DTO")
public record BoardRequestDto(

        @Schema(description = "게시글 제목", example = "오늘 시티팜에 상추 심었어요!")
        @NotBlank(message = "제목은 필수입니다.")
        String title,

        @Schema(description = "게시글 본문 내용", example = "씨앗부터 키우기 시작했는데 팁 공유해주세요.")
        @NotBlank(message = "내용은 필수입니다.")
        String content,

        @Schema(description = "게시글 카테고리", example = "FREE")
        @NotNull(message = "카테고리는 필수입니다.")
        Category category
) {
}