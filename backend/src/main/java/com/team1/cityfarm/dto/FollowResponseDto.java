package com.team1.cityfarm.dto;

import com.team1.cityfarm.entity.Follow;

import java.time.LocalDateTime;

public record FollowResponseDto(
        Long followId,
        Long userId,
        String nickname,
        LocalDateTime createdAt
) {
    public static FollowResponseDto from(Follow follow) {
        return new FollowResponseDto(
                follow.getId(),
                follow.getFollowing().getId(),
                follow.getFollowing().getNickname(),
                follow.getCreatedAt()
        );
    }
}
