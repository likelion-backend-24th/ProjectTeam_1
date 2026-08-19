package com.team1.cityfarm.repository;

import com.team1.cityfarm.entity.Rental;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RentalRepository extends JpaRepository<Rental, Long> {

    // 신청자 조회
    Page<Rental> findByUser_Id(Long userId, Pageable pageable);

    // 판매자 조회
    Page<Rental> findByFarm_Id(Long farmId, Pageable pageable);
}
