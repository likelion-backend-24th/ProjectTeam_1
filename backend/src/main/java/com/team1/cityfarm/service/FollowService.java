package com.team1.cityfarm.service;

import com.team1.cityfarm.dto.FollowResponseDto;
import com.team1.cityfarm.dto.FollowerResponseDto;
import com.team1.cityfarm.entity.Follow;
import com.team1.cityfarm.entity.User;
import com.team1.cityfarm.global.exception.CustomError;
import com.team1.cityfarm.global.exception.CustomException;
import com.team1.cityfarm.repository.FollowRepository;
import com.team1.cityfarm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FollowService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;

    // F-20 팔로우 등록
    @Transactional
    public void follow(Long loginUserId, Long followingId) {
        if (loginUserId.equals(followingId)) {
            throw new CustomException(CustomError.FOLLOW_SELF_NOT_ALLOWED);
        }

        User follower = userRepository.findById(loginUserId)
                .orElseThrow(() -> new CustomException(CustomError.USER_NOT_FOUND));
        User following = userRepository.findById(followingId)
                .orElseThrow(() -> new CustomException(CustomError.USER_NOT_FOUND));

        if (followRepository.existsByFollowerAndFollowing(follower, following)) {
            throw new CustomException(CustomError.FOLLOW_ALREADY_EXISTS);
        }

        followRepository.save(Follow.builder()
                .follower(follower)
                .following(following)
                .build());
    }

    // 팔로잉 목록 (내가 팔로우하는 사람들, 최신순)
    public List<FollowResponseDto> getFollowingList(Long userId) {
        return followRepository.findByFollowerIdOrderByCreatedAtDesc(userId).stream()
                .map(FollowResponseDto::from)
                .toList();
    }

    // 팔로워 목록 (나를 팔로우하는 사람들, 최신순)
    public List<FollowerResponseDto> getFollowerList(Long userId) {
        return followRepository.findByFollowingIdOrderByCreatedAtDesc(userId).stream()
                .map(FollowerResponseDto::from)
                .toList();
    }

    // F-22 팔로우 취소 (본인의 팔로우만 취소 가능)
    @Transactional
    public void unfollow(Long loginUserId, Long followId) {
        Follow follow = followRepository.findById(followId)
                .orElseThrow(() -> new CustomException(CustomError.FOLLOW_NOT_FOUND));

        if (!follow.getFollower().getId().equals(loginUserId)) {
            throw new CustomException(CustomError.FOLLOW_NOT_OWNER);
        }

        followRepository.delete(follow);
    }
}
