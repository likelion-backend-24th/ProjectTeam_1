package com.team1.cityfarm.service;

import com.team1.cityfarm.entity.*;
import com.team1.cityfarm.global.exception.CustomError;
import com.team1.cityfarm.global.exception.CustomException;
import com.team1.cityfarm.repository.ClassEnrollmentRepository;
import com.team1.cityfarm.repository.OneDayClassRepository;
import com.team1.cityfarm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClassEnrollmentService {

    private final ClassEnrollmentRepository classEnrollmentRepository;
    private final OneDayClassRepository oneDayClassRepository;
    private final UserRepository userRepository;

    /**
     * [일반 결제] PENDING 상태의 ClassEnrollment 생성
     */
    @Transactional
    public ClassEnrollment createPendingEnrollment(Long userId, Long classId, Order order) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(CustomError.USER_NOT_FOUND));

        OneDayClass oneDayClass = oneDayClassRepository.findById(classId)
                .orElseThrow(() -> new CustomException(CustomError.ONE_DAY_CLASS_NOT_FOUND));

        // 1. 이미 결제 진행 중이거나 수강 확정된 내역이 있는지 중복 검증
        boolean alreadyEnrolled = classEnrollmentRepository.existsByOneDayClassIdAndUserIdAndStatusIn(
                classId,
                userId,
                List.of(EnrollmentStatus.PENDING, EnrollmentStatus.CONFIRMED)
        );

        if (alreadyEnrolled) {
            throw new CustomException(CustomError.ALREADY_ENROLLED_CLASS);
        }

        // 2. PENDING 상태 수강 신청 객체 생성
        ClassEnrollment enrollment = ClassEnrollment.builder()
                .oneDayClass(oneDayClass)
                .user(user)
                .status(EnrollmentStatus.PENDING)
                .paymentType(PaymentType.GENERAL)
                .orderId(order.getId())
                .build();

        return classEnrollmentRepository.save(enrollment);
    }

    /**
     * [결제 완료 처리] PENDING -> CONFIRMED 상태 변경
     * (PaymentService 결제 검증 성공 후 호출)
     */
    @Transactional
    public ClassEnrollment confirmEnrollment(Long orderId) {
        ClassEnrollment enrollment = classEnrollmentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new CustomException(CustomError.ONE_DAY_CLASS_NOT_FOUND));

        enrollment.setStatus(EnrollmentStatus.CONFIRMED);

        return enrollment;
    }

    //수강권 사용 여부 확인
    //수강 일시가 현재 시각 이전이면 이미 진행된 클래스로 판단
    public boolean isEnrollmentUsed(Long orderId) {
        return classEnrollmentRepository.findByOrderId(orderId)
                .map(enrollment -> {
                    LocalDateTime classTime = enrollment.getOneDayClass().getDate();
                    return classTime != null && classTime.isBefore(LocalDateTime.now());
                })
                .orElse(false);
    }

   //수강 신청 취소 처리
   //(PaymentService 결제 취소 시 사용)
    @Transactional
    public void cancelEnrollment(Long orderId) {
        ClassEnrollment enrollment = classEnrollmentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new CustomException(CustomError.ONE_DAY_CLASS_NOT_FOUND));

        enrollment.setStatus(EnrollmentStatus.CANCELLED);
    }
}