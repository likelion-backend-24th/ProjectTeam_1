package com.team1.cityfarm.repository;

import com.team1.cityfarm.entity.BoardLike;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BoardLikeRepository extends JpaRepository<BoardLike, Long> {
    Optional<BoardLike> findByBoard_IdAndUser_Id(Long boardId, Long userId);
    boolean existsByBoard_IdAndUser_Id(Long boardId, Long userId);

    // F-50 좋아요한 게시글 목록 조회 (최신순)
    Page<BoardLike> findByUser_IdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
