package com.team1.cityfarm.repository;

import com.team1.cityfarm.entity.BoardComment;
import com.team1.cityfarm.entity.ReplyComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReplyCommentRepository extends JpaRepository<ReplyComment,Long> {

    List<ReplyComment> findByReplyIdOrderByCreatedAtAsc(Long boardId);
}
