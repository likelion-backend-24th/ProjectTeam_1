package com.team1.cityfarm.repository;

import com.team1.cityfarm.entity.Rental;
import com.team1.cityfarm.entity.RentalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RentalRepository extends JpaRepository<Rental, Long> {

    // 결제 주문 기준 조회
    Optional<Rental> findByOrderId(Long orderId);

    // 신청자 조회
    Page<Rental> findByUser_Id(Long userId, Pageable pageable);

    // 판매자 조회
    Page<Rental> findByFarm_Id(Long farmId, Pageable pageable);

    // 호스트 내가 등록한 밭들의 임대 목록
    Page<Rental> findByFarm_User_Id(Long hostUserId, Pageable pageable);

    // 특정 밭의 특정 상태 임대 건수 (호스트 관리 화면 카드용)
    long countByFarm_IdAndRentalStatus(Long farmId, RentalStatus rentalStatus);
}