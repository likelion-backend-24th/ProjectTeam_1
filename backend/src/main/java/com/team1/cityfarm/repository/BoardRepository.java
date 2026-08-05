package com.team1.cityfarm.repository;

import com.team1.cityfarm.entity.Board;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoardRepository extends JpaRepository<Board,Long> {
}
