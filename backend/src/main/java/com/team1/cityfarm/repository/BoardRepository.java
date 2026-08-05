package com.team1.cityfarm.repository;

import com.team1.cityfarm.entity.Board;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BoardRepository extends JpaRepository<Board, Long> {
    Page<Board> findByUser_Nickname(String keyword, Pageable pageable);
    Page<Board> findByTitleContaining(String keyword,Pageable pageable  );
    Page<Board> findByContentContaining(String keyword,Pageable pageable  );

}
