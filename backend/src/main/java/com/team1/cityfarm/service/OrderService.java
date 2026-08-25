package com.team1.cityfarm.service;

import com.team1.cityfarm.dto.FarmRentalOrderCreateRequestDto;
import com.team1.cityfarm.dto.OrderCreateRequestDto;
import com.team1.cityfarm.dto.OrderResponseDto;
import com.team1.cityfarm.entity.*;
import com.team1.cityfarm.global.exception.CustomError;
import com.team1.cityfarm.global.exception.CustomException;
import com.team1.cityfarm.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    // 결제창을 열지 않고 이탈하거나 결제 도중 이탈해도 별도 이벤트가 오지 않으므로,
    // 이 시간이 지나도록 PENDING인 원데이클래스 주문은 정원 점유를 풀어주기 위해 만료 처리한다.
    private static final long PENDING_ORDER_TTL_MINUTES = 30;

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final OneDayClassRepository oneDayClassRepository;
    private final ClassEnrollmentRepository classEnrollmentRepository;
    private final ClassEnrollmentService classEnrollmentService;
    private final PaymentRepository paymentRepository;
    private final FarmRepository farmRepository;
    private final RentalRepository rentalRepository;
    private final RentalService rentalService;

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
     * 밭 임대 결제 주문 생성
     */
    @Transactional
    public OrderResponseDto createFarmRentalOrder(Long userId, FarmRentalOrderCreateRequestDto requestDto){

        // 1. 유저 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(CustomError.USER_NOT_FOUND));

        // 2. 밭 임대 조회
        Farm farm = farmRepository.findById(requestDto.getFarmId())
                .orElseThrow(() -> new CustomException(CustomError.FARM_NOT_FOUND));

        int amount = farm.getMonthlyRent() * farm.getRentalMonths();

        // 3. 고유 merchantOrderId 생성
        String uuidSuffix = UUID.randomUUID().toString().substring(0,8).toUpperCase();
        String merchantOrderId = "BE24-CITYFARM-" + uuidSuffix;

        // 4. Order 생성
        Order order = Order.builder()
                .user(user)
                .amount(amount)
                .merchantOrderId(merchantOrderId)
                .orderType(OrderType.GENERAL)
                .orderStatus(OrderStatus.PENDING)
                .build();

        Order savedOrder = orderRepository.save(order);

        rentalService.createPendingRental(userId, farm.getId(), savedOrder, requestDto.getDescription());

        // 5. 응답
        return OrderResponseDto.from(savedOrder, farm.getTitle() + " 임대", null, null);
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

        LocalDateTime scheduledAt = null;
        String classTitle;

        if (order.getOrderType() == OrderType.SUBSCRIPTION) {
            classTitle = order.getSubscription().getPlanType().name() + " 정기구독";
        } else {
            ClassEnrollment enrollment = classEnrollmentRepository
                    .findByOrderId(order.getId())
                    .orElse(null);

            if (enrollment != null && enrollment.getOneDayClass() != null) {
                classTitle = enrollment.getOneDayClass().getTitle();
                scheduledAt = enrollment.getOneDayClass().getDate();
            } else {
                Rental rental = rentalRepository.findByOrderId(order.getId()).orElse(null);
                if (rental != null && rental.getFarm() != null) {
                    classTitle = rental.getFarm().getTitle() + " 임대";
                } else {
                    classTitle = "원데이클래스 신청";
                }
            }
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

            LocalDateTime scheduledAt = null;
            String classTitle;

            if (order.getOrderType() == OrderType.SUBSCRIPTION) {
                classTitle = order.getSubscription().getPlanType().name() + " 정기구독";
            } else {
                ClassEnrollment enrollment = classEnrollmentRepository
                        .findByOrderId(order.getId())
                        .orElse(null);

                if (enrollment != null && enrollment.getOneDayClass() != null) {
                    classTitle = enrollment.getOneDayClass().getTitle();
                    scheduledAt = enrollment.getOneDayClass().getDate();
                } else {
                    Rental rental = rentalRepository.findByOrderId(order.getId()).orElse(null);
                    if (rental != null && rental.getFarm() != null) {
                        classTitle = rental.getFarm().getTitle() + " 임대";
                    } else {
                        classTitle = "원데이클래스 신청";
                    }
                }
            }

            return OrderResponseDto.from(
                    order,
                    classTitle,
                    scheduledAt,
                    payment
            );
        });
    }

    /**
     * [사용자 직접 취소] 결제창을 열었지만 결제를 완료하지 않은(PENDING) 주문을 즉시 취소한다.
     * 아직 PortOne 결제가 없으므로(Payment row 미생성) 취소 API 호출 없이 주문/신청 상태만 되돌리면 된다.
     * TTL(30분) 만료 배치를 기다리지 않고도 정원 점유를 즉시 풀어주기 위한 진입점.
     */
    @Transactional
    public OrderResponseDto cancelPendingOrder(Long userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new CustomException(CustomError.ORDER_NOT_FOUND));

        if (!order.getUser().getId().equals(userId)) {
            throw new CustomException(CustomError.AUTH_UNAUTHORIZED);
        }

        if (order.getOrderType() != OrderType.GENERAL || order.getOrderStatus() != OrderStatus.PENDING) {
            throw new CustomException(CustomError.INVALID_ORDER_STATUS);
        }

        order.setOrderStatus(OrderStatus.CANCELLED);
        if (classEnrollmentService.hasEnrollmentForOrder(orderId)) {
            classEnrollmentService.cancelEnrollment(orderId);
        } else {
            rentalService.cancelRentalByOrderId(orderId);
        }

        log.info("[결제 대기 주문 취소] orderId: {}, merchantOrderId: {}", orderId, order.getMerchantOrderId());

        return getOrderDetails(userId, orderId);
    }

    /**
     * [배치] 결제 대기 상태로 TTL을 넘겨 방치된 원데이클래스 주문을 만료 처리한다.
     * 결제를 시도하지 않았거나 결제창을 닫고 이탈한 경우 PortOne 쪽에서 별도 웹훅이 오지 않으므로,
     * 주문을 결제 실패(FAILED)로, 연결된 신청을 취소로 전환해 정원 점유를 풀어준다.
     */
    @Transactional
    public int expireStalePendingOrders() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(PENDING_ORDER_TTL_MINUTES);
        List<Order> staleOrders = orderRepository.findByOrderTypeAndOrderStatusAndCreatedAtBefore(
                OrderType.GENERAL, OrderStatus.PENDING, cutoff
        );

        for (Order order : staleOrders) {
            order.setOrderStatus(OrderStatus.FAILED);
            if (classEnrollmentService.hasEnrollmentForOrder(order.getId())) {
                classEnrollmentService.cancelEnrollment(order.getId());
            } else {
                rentalService.cancelRentalByOrderId(order.getId());
            }
            log.info("[주문 만료 처리] 결제 대기 시간 초과로 주문을 만료 처리했습니다 - orderId: {}, merchantOrderId: {}",
                    order.getId(), order.getMerchantOrderId());
        }

        return staleOrders.size();
    }
}