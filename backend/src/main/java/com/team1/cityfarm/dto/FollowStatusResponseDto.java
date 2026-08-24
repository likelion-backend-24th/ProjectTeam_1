package com.team1.cityfarm.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "팔로우 상태 응답 DTO")
public record FollowStatusResponseDto(

        @Schema(description = "팔로우 여부", example = "true")
        boolean following,

        @Schema(description = "팔로우 ID (팔로우 중일 때만 존재, 취소 시 사용)", example = "1")
        Long followId
) {
}
