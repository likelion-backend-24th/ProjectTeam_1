package com.team1.cityfarm.repository;

import com.team1.cityfarm.entity.BoardComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BoardCommentRepository extends JpaRepository<BoardComment, Long> {


//    TODO : Board 연관관계 살아나면 주석해제
    List<BoardComment> findByBoardIdOrderByCreatedAtAsc(Long boardId);
}
