package com.team1.cityfarm.dto;

import com.team1.cityfarm.entity.Follow;

import java.time.LocalDateTime;

public record FollowerResponseDto(
        Long followId,
        Long userId,
        String nickname,
        LocalDateTime createdAt
) {
    public static FollowerResponseDto from(Follow follow) {
        return new FollowerResponseDto(
                follow.getId(),
                follow.getFollower().getId(),
                follow.getFollower().getNickname(),
                follow.getCreatedAt()
        );
    }
}
