package com.team1.cityfarm.service;

import com.team1.cityfarm.dto.PaymentCancelRequestDto;
import com.team1.cityfarm.dto.PaymentResponseDto;
import com.team1.cityfarm.dto.PaymentVerifyRequestDto;
import com.team1.cityfarm.entity.*;
import com.team1.cityfarm.global.exception.CustomError;
import com.team1.cityfarm.global.exception.CustomException;
import com.team1.cityfarm.portone.PortonePaymentClient;
import com.team1.cityfarm.portone.PortonePaymentResponseDto;
import com.team1.cityfarm.portone.RefundEligibilityResponseDto;
import com.team1.cityfarm.repository.OrderRepository;
import com.team1.cityfarm.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final PortonePaymentClient portOnePaymentClient;
    private final ClassEnrollmentService classEnrollmentService;
    private final SettlementService settlementService;

    /**
     * [PortOne 결제 결과 검증 및 승인 완료 처리]
     */
    @Transactional
    public PaymentResponseDto verifyAndCompletePayment(PaymentVerifyRequestDto request) {
        String paymentId = request.getPaymentId();
        String merchantOrderId = request.getMerchantOrderId();

        // 1. 이미 완료 처리된 PortOne 결제건인지 중복 확인
        if (paymentRepository.existsByPortonePaymentId(paymentId)) {
            throw new CustomException(CustomError.DUPLICATE_PAYMENT);
        }

        // 2. DB에서 해당 merchantOrderId로 주문 조회
        Order order = orderRepository.findByMerchantOrderId(merchantOrderId)
                .orElseThrow(() -> new CustomException(CustomError.ORDER_NOT_FOUND));

        // 3. 주문 상태가 PENDING인지 확인 (이미 웹훅 등에 의해 처리된 경우 중복 처리 방지)
        if (order.getOrderStatus() != OrderStatus.PENDING) {
            // 이미 PAID 상태라면 기존 결제 내역 반환
            if (order.getOrderStatus() == OrderStatus.PAID) {
                Payment existingPayment = paymentRepository.findByOrderId(order.getId())
                        .orElseThrow(() -> new CustomException(CustomError.PAYMENT_NOT_FOUND));
                return PaymentResponseDto.from(existingPayment);
            }
            throw new CustomException(CustomError.INVALID_ORDER_STATUS);
        }

        // 4. PortOne V2 API 단건 조회
        PortonePaymentResponseDto portOnePayment = portOnePaymentClient.getPaymentDetails(paymentId);

        // 5. [검증 ①] PortOne 결제 상태가 PAID인가?
        if (!"PAID".equalsIgnoreCase(portOnePayment.getStatus())) {
            log.error("[결제 검증 실패] PortOne 결제 미완료 상태 - paymentId: {}, status: {}", paymentId, portOnePayment.getStatus());
            throw new CustomException(CustomError.PAYMENT_FAILED);
        }

        // 6. [검증 ②] 실제 결제된 금액이 주문(Order) 금액과 일치하는가?
        Integer actualPaidAmount = portOnePayment.getAmount().getPaid();
        if (actualPaidAmount == null || !actualPaidAmount.equals(order.getAmount())) {
            log.error("[결제 위변조 위험] 결제 금액 불일치 - Order 금액: {}, PortOne 실제 결제 금액: {}", order.getAmount(), actualPaidAmount);
            throw new CustomException(CustomError.INVALID_PAYMENT_AMOUNT);
        }

        // 7~10. 결제 완료 승인 처리 (공통 메서드 호출)
        Payment payment = processPaymentSuccess(order, paymentId, actualPaidAmount, portOnePayment.getPayMethodType(), portOnePayment.getApprovedAtParsed());

        return PaymentResponseDto.from(payment);
    }

    /**
     * [공통 결제 성공 승인 로직]
     * Webhook과 verifyAndCompletePayment 양쪽에서 재사용 가능한 트랜잭션 단위 메서드
     */
    @Transactional
    public Payment processPaymentSuccess(Order order, String portonePaymentId, Integer amount, String payMethod, LocalDateTime approvedAt) {
        // 1. Payment 엔티티 생성 및 저장
        Payment payment = Payment.builder()
                .order(order)
                .portonePaymentId(portonePaymentId)
                .payMethod(payMethod)
                .amount(amount)
                .status(PaymentStatus.PAID)
                .approvedAt(approvedAt != null ? approvedAt : LocalDateTime.now())
                .build();

        Payment savedPayment = paymentRepository.save(payment);

        // 2. Order 상태 변경 (PENDING -> PAID)
        order.setOrderStatus(OrderStatus.PAID);

        // 3. ClassEnrollment 상태 변경 (PENDING -> CONFIRMED)
        ClassEnrollment enrollment = classEnrollmentService.confirmEnrollment(order.getId());

        // 4. Settlement 정산 데이터 생성
        if (enrollment != null && enrollment.getOneDayClass() != null) {
            settlementService.createPendingSettlement(order, enrollment.getOneDayClass().getId());
        }

        log.info("[결제 승인 완료] merchantOrderId: {}, paymentId: {}, amount: {}",
                order.getMerchantOrderId(), portonePaymentId, amount);

        return savedPayment;
    }

    /**
     * [환불 가능 여부 및 사유 조회]
     */
    public RefundEligibilityResponseDto checkRefundEligibility(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new CustomException(CustomError.PAYMENT_NOT_FOUND));

        if (payment.getStatus() == PaymentStatus.CANCELLED) {
            return RefundEligibilityResponseDto.fail("ALREADY_CANCELLED");
        }

        if (payment.getCreatedAt().plusHours(24).isBefore(LocalDateTime.now())) {
            return RefundEligibilityResponseDto.fail("REFUND_DEADLINE_EXCEEDED");
        }

        boolean isAlreadyUsed = classEnrollmentService.isEnrollmentUsed(payment.getOrder().getId());
        if (isAlreadyUsed) {
            return RefundEligibilityResponseDto.fail("PASS_ALREADY_USED");
        }

        return RefundEligibilityResponseDto.ok();
    }

    /**
     * [결제 전체 취소/환불]
     * userId 인자를 추가하여 결제자 소유권 검증 추가
     */
    @Transactional
    public PaymentResponseDto cancelPayment(Long userId, Long paymentId, PaymentCancelRequestDto request) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new CustomException(CustomError.PAYMENT_NOT_FOUND));

        // 결제 본인 검증
        if (!payment.getOrder().getUser().getId().equals(userId)) {
            throw new CustomException(CustomError.AUTH_UNAUTHORIZED);
        }

        // 1. 사전 환불 가능 여부 검증
        RefundEligibilityResponseDto eligibility = checkRefundEligibility(paymentId);
        if (!eligibility.isRefundable()) {
            log.error("[결제 취소 불가] paymentId: {}, reason: {}", paymentId, eligibility.getReason());
            throw new CustomException(CustomError.CANNOT_REFUND);
        }

        String cancelReason = (request != null && request.getReason() != null)
                ? request.getReason()
                : "사용자 요청에 의한 취소";

        // 2. PortOne V2 API 단건 결제 취소 요청
        portOnePaymentClient.cancelPayment(payment.getPortonePaymentId(), cancelReason);

        // 3. Payment 엔티티 상태 변경
        payment.cancel(cancelReason, LocalDateTime.now());

        // 4. Order 엔티티 상태 변경
        Order order = payment.getOrder();
        order.setOrderStatus(OrderStatus.CANCELLED);

        // 5. ClassEnrollment 상태 변경
        classEnrollmentService.cancelEnrollment(order.getId());

        log.info("[결제 취소 완료] paymentId: {}, merchantOrderId: {}, reason: {}",
                paymentId, order.getMerchantOrderId(), cancelReason);

        return PaymentResponseDto.from(payment);
    }
}