package com.team1.cityfarm.repository;

import com.team1.cityfarm.entity.Board;
import com.team1.cityfarm.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BoardRepository extends JpaRepository<Board, Long> {

    Page<Board> findByUser_Nickname(String keyword, Pageable pageable);

    Page<Board> findByTitleContaining(String keyword, Pageable pageable);

    Page<Board> findByContentContaining(String keyword, Pageable pageable);

    // 카테고리 필터 (검색어 없이 탭만 눌렀을 때)
    Page<Board> findByCategory(Category category, Pageable pageable);

    // 카테고리 + 검색어 조합
    Page<Board> findByCategoryAndTitleContaining(Category category, String keyword, Pageable pageable);

    Page<Board> findByCategoryAndContentContaining(Category category, String keyword, Pageable pageable);

    Page<Board> findByCategoryAndUser_Nickname(Category category, String keyword, Pageable pageable);

    // 피드 목록 조회 (팔로우한 사용자들의 게시글)
    Page<Board> findByUser_IdIn(List<Long> userIds, Pageable pageable);

}
