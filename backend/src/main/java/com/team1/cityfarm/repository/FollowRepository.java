package com.team1.cityfarm.repository;

import com.team1.cityfarm.entity.Follow;
import com.team1.cityfarm.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FollowRepository extends JpaRepository<Follow, Long> {

    boolean existsByFollowerAndFollowing(User follower, User following);

    Optional<Follow> findByFollowerAndFollowing(User follower, User following);

    // 내가 팔로우하는 사람들 (팔로잉 목록)
    List<Follow> findByFollowerIdOrderByCreatedAtDesc(Long followerId);

    // 나를 팔로우하는 사람들 (팔로워 목록)
    List<Follow> findByFollowingIdOrderByCreatedAtDesc(Long followingId);

    long countByFollowerId(Long followerId);
    long countByFollowingId(Long followingId);
}