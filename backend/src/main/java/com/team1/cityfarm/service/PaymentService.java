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

        // 3. 주문 상태가 PENDING인지 확인
        if (order.getOrderStatus() != OrderStatus.PENDING) {
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

        // 7. Payment 엔티티 생성 및 저장
        Payment payment = Payment.builder()
                .order(order)
                .portonePaymentId(paymentId)
                .amount(actualPaidAmount)
                .status(PaymentStatus.PAID)
                .build();

        Payment savedPayment = paymentRepository.save(payment);

        // 8. Order 상태 변경 (PENDING -> PAID)
        order.setOrderStatus(OrderStatus.PAID);

        // 9. ClassEnrollment 상태 변경 (PENDING -> CONFIRMED) 및 확정 객체 수령
        ClassEnrollment enrollment = classEnrollmentService.confirmEnrollment(order.getId());
        Long classId = enrollment.getOneDayClass().getId();

        // 10. Settlement 정산 데이터 생성 (Order, classId 매핑)
        settlementService.createPendingSettlement(order, classId);

        log.info("[결제 완료 성공] merchantOrderId: {}, paymentId: {}, amount: {}", merchantOrderId, paymentId, actualPaidAmount);

        return PaymentResponseDto.from(savedPayment);
    }

    /**
     * [환불 가능 여부 및 사유 조회]
     */
    public RefundEligibilityResponseDto checkRefundEligibility(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new CustomException(CustomError.PAYMENT_NOT_FOUND));

        // 1. 이미 취소된 결제건인지 검증
        if (payment.getStatus() == PaymentStatus.CANCELLED) {
            return RefundEligibilityResponseDto.fail("ALREADY_CANCELLED");
        }

        // 2. 결제 후 24시간 경과 여부 검증
        if (payment.getCreatedAt().plusHours(24).isBefore(LocalDateTime.now())) {
            return RefundEligibilityResponseDto.fail("REFUND_DEADLINE_EXCEEDED");
        }

        // 3. 수강/클래스 이미 진행(사용) 여부 검증
        boolean isAlreadyUsed = classEnrollmentService.isEnrollmentUsed(payment.getOrder().getId());
        if (isAlreadyUsed) {
            return RefundEligibilityResponseDto.fail("PASS_ALREADY_USED");
        }

        return RefundEligibilityResponseDto.ok();
    }


    //결제 전체 취소/환불
    @Transactional
    public PaymentResponseDto cancelPayment(Long paymentId, PaymentCancelRequestDto request) {

        // 1. 사전 환불 가능 여부 검증
        RefundEligibilityResponseDto eligibility = checkRefundEligibility(paymentId);
        if (!eligibility.isRefundable()) {
            log.error("[결제 취소 불가] paymentId: {}, reason: {}", paymentId, eligibility.getReason());
            throw new CustomException(CustomError.CANNOT_REFUND);
        }

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new CustomException(CustomError.PAYMENT_NOT_FOUND));

        // 삼항 연산자 수정 구문
        String cancelReason = (request != null && request.getReason() != null)
                ? request.getReason()
                : "사용자 요청에 의한 취소";

        // 2. PortOne V2 API 단건 결제 취소 요청
        portOnePaymentClient.cancelPayment(payment.getPortonePaymentId(), cancelReason);

        // 3. Payment 엔티티 상태 변경 (CANCELLED, 취소 사유/시간 기록)
        payment.cancel(cancelReason, LocalDateTime.now());

        // 4. Order 엔티티 상태 변경 (CANCELLED)
        Order order = payment.getOrder();
        order.setOrderStatus(OrderStatus.CANCELLED);

        // 5. ClassEnrollment 상태 변경 (CANCELLED)
        classEnrollmentService.cancelEnrollment(order.getId());

        log.info("[결제 취소 완료] paymentId: {}, merchantOrderId: {}, reason: {}",
                paymentId, order.getMerchantOrderId(), cancelReason);

        return PaymentResponseDto.from(payment);
    }
}