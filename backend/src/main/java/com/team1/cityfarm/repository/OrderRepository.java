package com.team1.cityfarm.repository;

import com.team1.cityfarm.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByMerchantOrderId(String merchantOrderId);

    boolean existsByMerchantOrderId(String merchantOrderId);

    List<Order> findByUserId(Long userId);

    Page<Order> findOrdersByUserId(Long userId, Pageable pageable);

    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<Order> findByIdAndUserId(Long orderId, Long userId);
}
