package com.team1.cityfarm.service;

import com.team1.cityfarm.dto.EnrollmentApplicantResponseDto;
import com.team1.cityfarm.dto.MyEnrollmentResponseDto;
import com.team1.cityfarm.entity.*;
import com.team1.cityfarm.global.exception.CustomError;
import com.team1.cityfarm.global.exception.CustomException;
import com.team1.cityfarm.repository.ClassEnrollmentRepository;
import com.team1.cityfarm.repository.OneDayClassRepository;
import com.team1.cityfarm.repository.OrderRepository;
import com.team1.cityfarm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Comparator;

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
    private final OrderRepository orderRepository;

    // 주문 건 구분
    public boolean hasEnrollmentForOrder(Long orderId){
        return classEnrollmentRepository.findByOrderId(orderId).isPresent();
    }

    // 정원 표시용 (PENDING 또는 CONFIRMED 상태인 신청 인원 수)
    public long getEnrolledCount(Long classId) {
        return classEnrollmentRepository.countByOneDayClassIdAndStatusIn(
                classId,
                List.of(EnrollmentStatus.PENDING, EnrollmentStatus.CONFIRMED)
        );
    }

    /**
     * [일반 결제] PENDING 상태의 ClassEnrollment 생성
     */
    @Transactional
    public ClassEnrollment createPendingEnrollment(Long userId, Long classId, Order order) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(CustomError.USER_NOT_FOUND));

        OneDayClass oneDayClass = lockAndValidateEnrollable(classId, userId);

        // PENDING 상태 수강 신청 객체 생성 (일반 결제는 PortOne 결제 승인 후 confirmEnrollment로 CONFIRMED 전환)
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
     * [구독 수강권 결제] 별도 결제 승인 없이 즉시 CONFIRMED 상태로 수강 신청을 생성한다.
     * 수강권 차감은 호출자(SubscriptionService)가 담당한다.
     */
    @Transactional
    public ClassEnrollment createConfirmedEnrollmentByPass(Long userId, Long classId, Long subscriptionId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(CustomError.USER_NOT_FOUND));

        OneDayClass oneDayClass = lockAndValidateEnrollable(classId, userId);

        ClassEnrollment enrollment = ClassEnrollment.builder()
                .oneDayClass(oneDayClass)
                .user(user)
                .status(EnrollmentStatus.CONFIRMED)
                .paymentType(PaymentType.SUBSCRIPTION)
                .subscriptionId(subscriptionId)
                .build();

        return classEnrollmentRepository.save(enrollment);
    }

    /**
     * 동시 신청으로 인한 정원 초과/중복 신청을 막기 위해 클래스 row에 락을 걸고
     * 같은 classId에 대한 동시 요청은 이 트랜잭션이 끝날 때까지 대기하게 되어
     * 중복/정원 체크가 순차적으로 진행되도록 보장한다.
     */
    private OneDayClass lockAndValidateEnrollable(Long classId, Long userId) {
        OneDayClass oneDayClass = oneDayClassRepository.findByIdForUpdate(classId)
                .orElseThrow(() -> new CustomException(CustomError.ONE_DAY_CLASS_NOT_FOUND));

        if (oneDayClass.getHost().getId().equals(userId)) {
            throw new CustomException(CustomError.CLASS_HOST_CANNOT_ENROLL);
        }

        List<ClassEnrollment> activeEnrollments = classEnrollmentRepository.findByOneDayClassIdAndUserIdAndStatusIn(
                classId,
                userId,
                List.of(EnrollmentStatus.PENDING, EnrollmentStatus.CONFIRMED)
        );

        for (ClassEnrollment existing : activeEnrollments) {
            if (existing.getStatus() == EnrollmentStatus.CONFIRMED) {
                throw new CustomException(CustomError.ALREADY_ENROLLED_CLASS);
            }

            // PENDING: 결제창까지 가지 않고 이탈한 이전 시도로 보고, 그 신청/주문을 정리한 뒤
            // 이번 신청을 새로 진행한다. 유예 시간 없이 즉시 정리한다 - 결제 완료 여부는 이미
            // order/paymentId 단위(merchantOrderId)로 PortOne과 매칭되므로, 여기서 취소해도
            // 그 사이 실제로 결제가 성사됐다면 verifyAndCompletePayment가 주문 상태 불일치로 거부한다.
            existing.setStatus(EnrollmentStatus.CANCELLED);
            if (existing.getOrderId() != null) {
                orderRepository.findById(existing.getOrderId())
                        .filter(order -> order.getOrderStatus() == OrderStatus.PENDING)
                        .ifPresent(order -> order.setOrderStatus(OrderStatus.CANCELLED));
            }
        }

        long enrolledCount = classEnrollmentRepository.countByOneDayClassIdAndStatusIn(
                classId,
                List.of(EnrollmentStatus.PENDING, EnrollmentStatus.CONFIRMED)
        );

        if (enrolledCount >= oneDayClass.getCapacity()) {
            throw new CustomException(CustomError.CLASS_CAPACITY_EXCEEDED);
        }

        return oneDayClass;
    }


    @Transactional
    public ClassEnrollment confirmEnrollment(Long orderId) {
        ClassEnrollment enrollment = classEnrollmentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new CustomException(CustomError.ENROLLMENT_NOT_FOUND));

        if (enrollment.getStatus() == EnrollmentStatus.CANCELLED) {
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
    public List<MyEnrollmentResponseDto> getMyEnrollment(Long userId) {
        return classEnrollmentRepository.findByUser_Id(userId).stream()
                .filter(e -> e.getStatus() == EnrollmentStatus.CONFIRMED)
                .filter(e -> e.getOneDayClass().getDate().isAfter(LocalDateTime.now()))
                .sorted(Comparator.comparing(e -> e.getOneDayClass().getDate()))
                .limit(3)
                .map(MyEnrollmentResponseDto::from)
                .collect(Collectors.toList());
    }

    //    호스트 - 내 클래스 신청자 목록 조회
    public List<EnrollmentApplicantResponseDto> getApplicant(Long classId, Long hostId) {
        OneDayClass oneDayClass = oneDayClassRepository.findById(classId)
                .orElseThrow(() -> new CustomException(CustomError.ONE_DAY_CLASS_NOT_FOUND));

        if (!oneDayClass.getHost().getId().equals(hostId)) {
            throw new CustomException(CustomError.CLASS_NOT_OWNER);
        }

        return classEnrollmentRepository.findByOneDayClass_Id(classId).stream()
                .map(EnrollmentApplicantResponseDto::from)
                .collect(Collectors.toList());
    }

    /*    구독 수강권 신청 취소 enrollmentId 기준으로 조회(orderId/subcriptionId는 유일하지 않아 사용 불가)
        수강권 복구는 호출자(subcriptionService)가 담당.*/
    @Transactional
    public ClassEnrollment cancelEnrollmentByPass(Long enrollmentId, Long userId) {

        ClassEnrollment classEnrollment = classEnrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new CustomException(CustomError.ENROLLMENT_NOT_FOUND));

        if (!classEnrollment.getUser().getId().equals(userId)) {
            throw new CustomException(CustomError.ENROLLMENT_NOT_OWNER);
        }

        if (classEnrollment.getPaymentType() != PaymentType.SUBSCRIPTION) {
            throw new CustomException(CustomError.INVALID_ORDER_STATUS);
        }

        if (classEnrollment.getStatus() == EnrollmentStatus.CANCELLED) {
            return classEnrollment;
        }

        classEnrollment.setStatus(EnrollmentStatus.CANCELLED);
        return classEnrollment;
    }

    @Transactional
    public List<ClassEnrollment> cancelAllEnrollmentsForClass(Long classId) {
        List<ClassEnrollment> targets = classEnrollmentRepository.findByOneDayClass_Id(classId).stream()
                .filter(e -> e.getStatus() == EnrollmentStatus.PENDING || e.getStatus() == EnrollmentStatus.CONFIRMED)
                .collect(Collectors.toList());

        targets.forEach(e -> e.setStatus(EnrollmentStatus.CANCELLED));

        return targets;
    }
}