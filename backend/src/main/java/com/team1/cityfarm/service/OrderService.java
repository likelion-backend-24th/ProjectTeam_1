package com.team1.cityfarm.service;

import com.team1.cityfarm.dto.OrderCreateRequestDto;
import com.team1.cityfarm.dto.OrderResponseDto;
import com.team1.cityfarm.entity.*;
import com.team1.cityfarm.global.exception.CustomError;
import com.team1.cityfarm.global.exception.CustomException;
import com.team1.cityfarm.repository.OneDayClassRepository;
import com.team1.cityfarm.repository.OrderRepository;
import com.team1.cityfarm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;
    private final OneDayClassRepository oneDayClassRepository;
    private final UserRepository userRepository;
    private final ClassEnrollmentService classEnrollmentService;

    @Transactional
    public OrderResponseDto createClassOrder(Long userId, OrderCreateRequestDto request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(CustomError.USER_NOT_FOUND));

        OneDayClass oneDayClass = oneDayClassRepository.findById(request.getClassId())
                .orElseThrow(() -> new CustomException(CustomError.ONE_DAY_CLASS_NOT_FOUND));

        String merchantOrderId = "BE24-CITYFARM-" + UUID.randomUUID();

        Order order = Order.builder()
                .user(user)
                .amount(oneDayClass.getPrice())
                .merchantOrderId(merchantOrderId)
                .orderType(OrderType.GENERAL)
                .orderStatus(OrderStatus.PENDING)
                .build();

        Order savedOrder = orderRepository.save(order);

        //  PENDING 수강신청을 같이 생성
        classEnrollmentService.createPendingEnrollment(userId, request.getClassId(), savedOrder);

        return OrderResponseDto.from(savedOrder);
    }
}