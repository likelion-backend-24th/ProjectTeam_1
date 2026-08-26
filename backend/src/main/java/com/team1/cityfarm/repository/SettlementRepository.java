package com.team1.cityfarm.repository;

import com.team1.cityfarm.entity.Settlement;
import com.team1.cityfarm.entity.SettlementStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SettlementRepository
        extends JpaRepository<Settlement, Long> {

    List<Settlement> findByHostId(Long hostId);

    List<Settlement> findByHostIdOrderByCreatedAtDesc(Long hostId);

    List<Settlement> findByHostIdAndStatus(
            Long hostId,
            SettlementStatus status
    );

    Optional<Settlement> findByOrderId(Long orderId);

    boolean existsByOrderId(Long orderId);

    Optional<Settlement> findByEnrollmentId(Long enrollmentId);

    // 관리자용: 전체 정산 내역 조회 (최신순)
    List<Settlement> findAllByOrderByIdDesc();
}
