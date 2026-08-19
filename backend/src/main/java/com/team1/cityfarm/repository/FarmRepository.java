package com.team1.cityfarm.repository;

import com.team1.cityfarm.entity.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FarmRepository extends JpaRepository<Farm, Long> {

    // 내가 등록한 밭 목록 (마이 페이지로 사용 가능)
    Page<Farm> findByUser_Id(Long userId, Pageable pageable);

    // 임대 가능한 밭 조회
    Page<Farm> findByFarmStatus(FarmStatus farmStatus, Pageable pageable);

    // 지역명으로 검색
    Page<Farm> findByLocationContaining(String location, Pageable pageable);

    // 타이틀로 검색
    Page<Farm> findByTitleContaining(String title, Pageable pageable);
}