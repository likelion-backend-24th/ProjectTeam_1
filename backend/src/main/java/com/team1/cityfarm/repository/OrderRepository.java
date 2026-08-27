package com.team1.cityfarm.repository;

import com.team1.cityfarm.entity.Order;
import com.team1.cityfarm.entity.OrderStatus;
import com.team1.cityfarm.entity.OrderType;
import com.team1.cityfarm.entity.Rental;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByMerchantOrderId(String merchantOrderId);

    boolean existsByMerchantOrderId(String merchantOrderId);

    List<Order> findByUserId(Long userId);

    Page<Order> findOrdersByUserId(Long userId, Pageable pageable);

    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<Order> findByIdAndUserId(Long orderId, Long userId);

    // 구독의 "현재 회차" 결제건 조회용 (가입/갱신 시마다 새 Order가 쌓이므로 가장 최근 것을 사용)
    Optional<Order> findTopBySubscriptionIdOrderByCreatedAtDesc(Long subscriptionId);

    // 결제창을 열지 않고 이탈하는 등, 결제 대기 상태로 일정 시간 이상 방치된 원데이클래스 주문 조회용
    List<Order> findByOrderTypeAndOrderStatusAndCreatedAtBefore(
            OrderType orderType,
            OrderStatus orderStatus,
            LocalDateTime cutoff
    );


}
