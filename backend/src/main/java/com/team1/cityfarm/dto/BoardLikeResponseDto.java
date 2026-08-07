package com.team1.cityfarm.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "게시글 좋아요 응답 DTO")
public record BoardLikeResponseDto(

        @Schema(description = "좋아요 여부", example = "true")
        boolean liked,

        @Schema(description = "좋아요 개수", example = "24")
        int likeCount
) {
}
