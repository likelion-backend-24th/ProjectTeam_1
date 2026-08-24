package com.team1.cityfarm.repository;

import com.team1.cityfarm.entity.BoardImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BoardImageRepository extends JpaRepository<BoardImage, Long> {

    // 사진 전체 목록 (등록 순서대로)
    List<BoardImage> findByBoard_IdOrderByIdAsc(Long boardId);

    // 여러 게시글의 사진을 한 번에 조회 (목록 조회 시 N+1 방지)
    List<BoardImage> findByBoard_IdInOrderByIdAsc(List<Long> boardIds);
}
