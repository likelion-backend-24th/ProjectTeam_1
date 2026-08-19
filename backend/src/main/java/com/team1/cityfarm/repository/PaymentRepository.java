package com.team1.cityfarm.repository;

import com.team1.cityfarm.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository
        extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrderId(Long orderId);

    Optional<Payment> findByPortonePaymentId(String portonePaymentId);

    List<Payment> findByOrderUserId(Long userId);

    List<Payment> findByOrderUserIdOrderByCreatedAtDesc(Long userId);

    boolean existsByPortonePaymentId(String portonePaymentId);
}