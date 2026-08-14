package com.team1.cityfarm.repository;

import com.team1.cityfarm.entity.ClassEnrollment;
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
}
