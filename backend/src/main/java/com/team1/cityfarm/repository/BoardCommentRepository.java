package com.team1.cityfarm.repository;

import com.team1.cityfarm.entity.BoardComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BoardCommentRepository extends JpaRepository<BoardComment, Long> {

    List<BoardComment> findByBoardIdOrderByCreatedAtAsc(Long boardId);
}
