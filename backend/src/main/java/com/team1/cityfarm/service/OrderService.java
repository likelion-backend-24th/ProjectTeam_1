package com.team1.cityfarm.service;

import com.team1.cityfarm.dto.OrderCreateRequestDto;
import com.team1.cityfarm.dto.OrderResponseDto;
import com.team1.cityfarm.entity.*;
import com.team1.cityfarm.global.exception.CustomError;
import com.team1.cityfarm.global.exception.CustomException;
import com.team1.cityfarm.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final OneDayClassRepository oneDayClassRepository;
    private final ClassEnrollmentRepository classEnrollmentRepository;
    private final ClassEnrollmentService classEnrollmentService;
    private final PaymentRepository paymentRepository;

    /**
     * 원데이 클래스 일반 결제 주문 생성
     */
    @Transactional
    public OrderResponseDto createClassOrder(
            Long userId,
            OrderCreateRequestDto requestDto
    ) {

        // 1. 유저 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(CustomError.USER_NOT_FOUND));

        // 2. 원데이 클래스 조회
        OneDayClass oneDayClass = oneDayClassRepository.findById(requestDto.getClassId())
                .orElseThrow(() -> new CustomException(CustomError.ONE_DAY_CLASS_NOT_FOUND));

        // 3. 고유 merchantOrderId 생성
        String uuidSuffix = UUID.randomUUID()
                .toString()
                .substring(0, 8)
                .toUpperCase();

        String merchantOrderId = "BE24-CITYFARM-" + uuidSuffix;

        // 4. Order 생성
        Order order = Order.builder()
                .user(user)
                .amount(oneDayClass.getPrice())
                .merchantOrderId(merchantOrderId)
                .orderType(OrderType.GENERAL)
                .orderStatus(OrderStatus.PENDING)
                .build();

        Order savedOrder = orderRepository.save(order);

        // 5. ClassEnrollment 생성
        // 현재 ClassEnrollmentService의 기존 메서드를 사용
        // 중복 신청 검증도 해당 서비스에서 처리
        classEnrollmentService.createPendingEnrollment(
                userId,
                oneDayClass.getId(),
                savedOrder
        );

        // 6. 응답
        return OrderResponseDto.from(
                savedOrder,
                oneDayClass.getTitle(),
                oneDayClass.getDate(),
                null
        );
    }

    /**
     * 단건 주문 상세 조회
     */
    public OrderResponseDto getOrderDetails(Long userId, Long orderId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new CustomException(CustomError.ORDER_NOT_FOUND));

        if (!order.getUser().getId().equals(userId)) {
            throw new CustomException(CustomError.AUTH_UNAUTHORIZED);
        }

        Payment payment = paymentRepository
                .findByOrderId(order.getId())
                .orElse(null);

        String classTitle = "원데이클래스 신청";
        LocalDateTime scheduledAt = null;

        ClassEnrollment enrollment = classEnrollmentRepository
                .findByOrderId(order.getId())
                .orElse(null);

        if (enrollment != null && enrollment.getOneDayClass() != null) {
            classTitle = enrollment.getOneDayClass().getTitle();
            scheduledAt = enrollment.getOneDayClass().getDate();
        }

        return OrderResponseDto.from(
                order,
                classTitle,
                scheduledAt,
                payment
        );
    }

    /**
     * 내 주문 내역 목록 조회
     */
    public Page<OrderResponseDto> getMyOrders(
            Long userId,
            Pageable pageable
    ) {

        Page<Order> orders =
                orderRepository.findOrdersByUserId(userId, pageable);

        return orders.map(order -> {

            Payment payment = paymentRepository
                    .findByOrderId(order.getId())
                    .orElse(null);

            String classTitle = "원데이클래스 신청";
            LocalDateTime scheduledAt = null;

            ClassEnrollment enrollment = classEnrollmentRepository
                    .findByOrderId(order.getId())
                    .orElse(null);

            if (enrollment != null && enrollment.getOneDayClass() != null) {
                classTitle = enrollment.getOneDayClass().getTitle();
                scheduledAt = enrollment.getOneDayClass().getDate();
            }

            return OrderResponseDto.from(
                    order,
                    classTitle,
                    scheduledAt,
                    payment
            );
        });
    }
}