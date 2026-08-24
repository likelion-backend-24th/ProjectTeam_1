package com.team1.cityfarm.repository;

import com.team1.cityfarm.entity.ClassEnrollment;
import com.team1.cityfarm.entity.EnrollmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ClassEnrollmentRepository extends JpaRepository<ClassEnrollment,Long> {

//    원데이클래스 신청 목록
    List<ClassEnrollment> findByOneDayClass_Id(Long oneDayClassId);

//    원데이클래스 유저 신청 내역 조회(마이페이지)
    List<ClassEnrollment> findByUser_Id(Long userId);

    // orderId 기반 수강신청 조회 (결제 성공 후 PENDING -> CONFIRMED 변경 시 사용)
    Optional<ClassEnrollment> findByOrderId(Long orderId);

    // 중복 신청/결제 이탈로 남은 PENDING 신청 판별용 (PENDING 또는 CONFIRMED 상태인 신청 조회)
    List<ClassEnrollment> findByOneDayClassIdAndUserIdAndStatusIn(
            Long classId,
            Long userId,
            List<EnrollmentStatus> statuses
    );

    // 정원확인용 (PENDING 또는 CONFIRMED 상태인 신청 인원 수 확인)
    long countByOneDayClassIdAndStatusIn(
            Long classId,
            List<EnrollmentStatus> statuses
    );

    // 마이 페이지 - 다가오는 클래스(확정된 것 준 현재 이후, 날짜 빠른 순 최대 3개)
    List<ClassEnrollment> findTop3ByUser_IdAndStatusAndOneDayClass_DateAfterOrderByOneDayClass_DateAsc(
        Long userId,
        EnrollmentStatus status,
        LocalDateTime now
    );
}
