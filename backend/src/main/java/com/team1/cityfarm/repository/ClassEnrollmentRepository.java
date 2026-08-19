package com.team1.cityfarm.repository;

import com.team1.cityfarm.entity.ClassEnrollment;
import com.team1.cityfarm.entity.EnrollmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClassEnrollmentRepository extends JpaRepository<ClassEnrollment,Long> {

//    원데이클래스 신청 목록
    List<ClassEnrollment> findByOneDayClass_Id(Long oneDayClassId);

//    원데이클래스 유저 신청 내역 조회(마이페이지)
    List<ClassEnrollment> findByUser_Id(Long userId);

//    중복신청 확인용
    Optional<ClassEnrollment> findByOneDayClass_IdAndUser_Id(Long onDayClassId, Long userId);

    // orderId 기반 수강신청 조회 (결제 성공 후 PENDING -> CONFIRMED 변경 시 사용)
    Optional<ClassEnrollment> findByOrderId(Long orderId);

    // 중복 신청 방지 검증용 (PENDING 또는 CONFIRMED 상태인 신청이 존재하는지 확인)
    boolean existsByOneDayClassIdAndUserIdAndStatusIn(
            Long classId,
            Long userId,
            java.util.List<EnrollmentStatus> statuses
    );
}
