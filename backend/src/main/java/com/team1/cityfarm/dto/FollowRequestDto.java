package com.team1.cityfarm.dto;

import jakarta.validation.constraints.NotNull;

public record FollowRequestDto(

        @NotNull(message = "팔로우할 사용자를 입력해주세요")
        Long followingId
) {
}
