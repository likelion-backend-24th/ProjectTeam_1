package com.team1.cityfarm.repository;

import com.team1.cityfarm.entity.Reply;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReplyRepository extends JpaRepository<Reply, Long> {
    public Optional<List<Reply>> findAllByBoard_Id(Long id);
    public Optional<Reply> findReplyByBoard_IdAndUser_Id(Long boardId, Long userId);
    public Optional<Reply> findReplyByIdAndUser_Id(Long id, Long userId);
}
