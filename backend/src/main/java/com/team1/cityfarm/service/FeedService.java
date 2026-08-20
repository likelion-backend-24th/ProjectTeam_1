package com.team1.cityfarm.service;

import com.team1.cityfarm.dto.BoardResponseDto;
import com.team1.cityfarm.repository.BoardRepository;
import com.team1.cityfarm.repository.FollowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedService {

    private final FollowRepository followRepository;
    private final BoardRepository boardRepository;

    // F-30 피드 목록 조회 (내가 팔로우한 사용자들의 게시글, 최신순)
    public Page<BoardResponseDto> getFeed(Long userId, Pageable pageable) {
        List<Long> followingIds = followRepository.findByFollowerIdOrderByCreatedAtDesc(userId).stream()
                .map(follow -> follow.getFollowing().getId())
                .toList();

        if (followingIds.isEmpty()) {
            return Page.empty(pageable);
        }

        return boardRepository.findByUser_IdIn(followingIds, pageable)
                .map(BoardResponseDto::from);
    }
}
