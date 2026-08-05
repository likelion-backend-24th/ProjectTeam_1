package com.team1.cityfarm.repository;

import com.team1.cityfarm.entity.BoardComment;
import com.team1.cityfarm.entity.ReplyComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReplyCommentRepository extends JpaRepository<ReplyComment,Long> {

//        TODO : Reply 연관관계 살아나면 주석해제
//    List<BoardComment> findByReplyIdOrderByCreatedAtAsc(Long boardId);
}
