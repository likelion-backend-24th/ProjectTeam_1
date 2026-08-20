package com.team1.cityfarm.service;

import com.team1.cityfarm.dto.EnrollmentApplicantResponseDto;
import com.team1.cityfarm.dto.MyEnrollmentResponseDto;
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
import java.util.stream.Collectors;

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

         /*동시 신청으로 인한 정원 초과/중복 신청을 막기 위해 클래스 row에 락을 걸고
         같은 classId에 대한 동시 요청은 이 트랜잭션이 끝날 때까지 대기하게 되어
         아래 중복/정원 체크가 순차적 진행.*/
        OneDayClass oneDayClass = oneDayClassRepository.findByIdForUpdate(classId)
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

        // 2. 정원 초과 여부 검증
        long enrolledCount = classEnrollmentRepository.countByOneDayClassIdAndStatusIn(
                classId,
                List.of(EnrollmentStatus.PENDING, EnrollmentStatus.CONFIRMED)
        );

        if (enrolledCount >= oneDayClass.getCapacity()) {
            throw new CustomException(CustomError.CLASS_CAPACITY_EXCEEDED);
        }

        // 3. PENDING 상태 수강 신청 객체 생성
        ClassEnrollment enrollment = ClassEnrollment.builder()
                .oneDayClass(oneDayClass)
                .user(user)
                .status(EnrollmentStatus.PENDING)
                .paymentType(PaymentType.GENERAL)
                .orderId(order.getId())
                .build();

        return classEnrollmentRepository.save(enrollment);
    }


    @Transactional
    public ClassEnrollment confirmEnrollment(Long orderId) {
        ClassEnrollment enrollment = classEnrollmentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new CustomException(CustomError.ENROLLMENT_NOT_FOUND));

        if (enrollment.getStatus() == EnrollmentStatus.CANCELLED){
            throw new CustomException(CustomError.INVALID_ORDER_STATUS);
        }

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
   @Transactional
   public void cancelEnrollment(Long orderId) {
       ClassEnrollment enrollment = classEnrollmentRepository.findByOrderId(orderId)
               .orElseThrow(() -> new CustomException(CustomError.ENROLLMENT_NOT_FOUND));

       if (enrollment.getStatus() == EnrollmentStatus.CANCELLED) {
           return;
       }

       enrollment.setStatus(EnrollmentStatus.CANCELLED);
   }

//    마이페이지 - 내 신청내역 조회
    public List<MyEnrollmentResponseDto> getMyEnrollment(Long userId){
        return classEnrollmentRepository.findByUser_Id(userId).stream()
                .map(MyEnrollmentResponseDto::from)
                .collect(Collectors.toList());
    }

//    호스트 - 내 클래스 신청자 목록 조회
    public List<EnrollmentApplicantResponseDto> getApplicant(Long classId,Long hostId){
        OneDayClass oneDayClass = oneDayClassRepository.findById(classId)
                .orElseThrow(()-> new CustomException(CustomError.ONE_DAY_CLASS_NOT_FOUND));

        if (!oneDayClass.getHost().getId().equals(hostId)){
            throw new CustomException(CustomError.CLASS_NOT_OWNER);
        }

        return classEnrollmentRepository.findByOneDayClass_Id(classId).stream()
                .map(EnrollmentApplicantResponseDto::from)
                .collect(Collectors.toList());
    }


}