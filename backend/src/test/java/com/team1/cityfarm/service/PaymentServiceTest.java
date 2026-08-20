package com.team1.cityfarm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team1.cityfarm.dto.PaymentCancelRequestDto;
import com.team1.cityfarm.dto.PaymentVerifyRequestDto;
import com.team1.cityfarm.entity.*;
import com.team1.cityfarm.global.exception.CustomError;
import com.team1.cityfarm.global.exception.CustomException;
import com.team1.cityfarm.portone.PortonePaymentClient;
import com.team1.cityfarm.portone.PortonePaymentResponseDto;
import com.team1.cityfarm.portone.RefundEligibilityResponseDto;
import com.team1.cityfarm.repository.OrderRepository;
import com.team1.cityfarm.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private PortonePaymentClient portOnePaymentClient;
    @Mock private ClassEnrollmentService classEnrollmentService;
    @Mock private SettlementService settlementService;

    private PaymentService service;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        service = new PaymentService(paymentRepository, orderRepository, portOnePaymentClient,
                classEnrollmentService, settlementService);
    }

    private PaymentVerifyRequestDto verifyRequest(String paymentId, String merchantOrderId) {
        PaymentVerifyRequestDto dto = new PaymentVerifyRequestDto();
        ReflectionTestUtils.setField(dto, "paymentId", paymentId);
        ReflectionTestUtils.setField(dto, "merchantOrderId", merchantOrderId);
        return dto;
    }

    private PortonePaymentResponseDto portOneResponse(String status, Integer paid) throws Exception {
        String json = """
                {"id":"pay_x","status":"%s","paidAt":"2026-08-20T00:00:00Z",
                 "amount":{"total":%s,"paid":%s},"method":{"type":"Card","provider":"CARD"}}
                """.formatted(status, paid, paid);
        return objectMapper.readValue(json, PortonePaymentResponseDto.class);
    }

    private User user(Long id) {
        return User.builder().id(id).email("u@test.com").password("pw").nickname("nick").build();
    }

    private Order order(Long id, User u, int amount, OrderStatus status, OrderType type) {
        return Order.builder().id(id).user(u).amount(amount).merchantOrderId("m_1")
                .orderType(type).orderStatus(status).build();
    }

    // ===== verifyAndCompletePayment =====

    @Test
    void 이미_처리된_PortOne결제ID면_중복예외() {
        when(paymentRepository.existsByPortonePaymentId("pay_1")).thenReturn(true);

        assertThatThrownBy(() -> service.verifyAndCompletePayment(verifyRequest("pay_1", "m_1")))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getCustomError()).isEqualTo(CustomError.DUPLICATE_PAYMENT));
    }

    @Test
    void 주문이_없으면_예외() {
        when(paymentRepository.existsByPortonePaymentId("pay_1")).thenReturn(false);
        when(orderRepository.findByMerchantOrderId("m_1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.verifyAndCompletePayment(verifyRequest("pay_1", "m_1")))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getCustomError()).isEqualTo(CustomError.ORDER_NOT_FOUND));
    }

    @Test
    void 주문이_이미_PAID면_기존_결제내역을_반환하고_PortOne을_다시_안_부른다() {
        User u = user(1L);
        Order o = order(1L, u, 30000, OrderStatus.PAID, OrderType.GENERAL);
        Payment existing = Payment.builder().id(9L).order(o).portonePaymentId("pay_1")
                .amount(30000).status(PaymentStatus.PAID).createdAt(LocalDateTime.now()).build();

        when(paymentRepository.existsByPortonePaymentId("pay_1")).thenReturn(false);
        when(orderRepository.findByMerchantOrderId("m_1")).thenReturn(Optional.of(o));
        when(paymentRepository.findByOrderId(1L)).thenReturn(Optional.of(existing));

        service.verifyAndCompletePayment(verifyRequest("pay_1", "m_1"));

        verifyNoInteractions(portOnePaymentClient);
    }

    @Test
    void 주문이_PENDING도_PAID도_아니면_예외() {
        User u = user(1L);
        Order o = order(1L, u, 30000, OrderStatus.CANCELLED, OrderType.GENERAL);

        when(paymentRepository.existsByPortonePaymentId("pay_1")).thenReturn(false);
        when(orderRepository.findByMerchantOrderId("m_1")).thenReturn(Optional.of(o));

        assertThatThrownBy(() -> service.verifyAndCompletePayment(verifyRequest("pay_1", "m_1")))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getCustomError()).isEqualTo(CustomError.INVALID_ORDER_STATUS));
    }

    @Test
    void PortOne_상태가_PAID가_아니면_예외() throws Exception {
        User u = user(1L);
        Order o = order(1L, u, 30000, OrderStatus.PENDING, OrderType.GENERAL);

        when(paymentRepository.existsByPortonePaymentId("pay_1")).thenReturn(false);
        when(orderRepository.findByMerchantOrderId("m_1")).thenReturn(Optional.of(o));
        when(portOnePaymentClient.getPaymentDetails("pay_1")).thenReturn(portOneResponse("READY", 30000));

        assertThatThrownBy(() -> service.verifyAndCompletePayment(verifyRequest("pay_1", "m_1")))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getCustomError()).isEqualTo(CustomError.PAYMENT_FAILED));
    }

    @Test
    void 결제금액이_주문금액과_다르면_예외() throws Exception {
        User u = user(1L);
        Order o = order(1L, u, 30000, OrderStatus.PENDING, OrderType.GENERAL);

        when(paymentRepository.existsByPortonePaymentId("pay_1")).thenReturn(false);
        when(orderRepository.findByMerchantOrderId("m_1")).thenReturn(Optional.of(o));
        when(portOnePaymentClient.getPaymentDetails("pay_1")).thenReturn(portOneResponse("PAID", 100));

        assertThatThrownBy(() -> service.verifyAndCompletePayment(verifyRequest("pay_1", "m_1")))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getCustomError()).isEqualTo(CustomError.INVALID_PAYMENT_AMOUNT));
    }

    @Test
    void 정상_검증시_결제승인처리_되고_클래스면_정산도_생성된다() throws Exception {
        User u = user(1L);
        Order o = order(1L, u, 30000, OrderStatus.PENDING, OrderType.GENERAL);
        OneDayClass clazz = OneDayClass.builder().id(7L).build();
        ClassEnrollment enrollment = ClassEnrollment.builder().id(3L).oneDayClass(clazz).build();

        when(paymentRepository.existsByPortonePaymentId("pay_1")).thenReturn(false);
        when(orderRepository.findByMerchantOrderId("m_1")).thenReturn(Optional.of(o));
        when(portOnePaymentClient.getPaymentDetails("pay_1")).thenReturn(portOneResponse("PAID", 30000));
        when(classEnrollmentService.confirmEnrollment(1L)).thenReturn(enrollment);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.verifyAndCompletePayment(verifyRequest("pay_1", "m_1"));

        assertThat(o.getOrderStatus()).isEqualTo(OrderStatus.PAID);
        verify(paymentRepository).save(any(Payment.class));
        verify(settlementService).createPendingSettlement(o, 7L);
    }

    // ===== checkRefundEligibility =====

    @Test
    void 이미_취소된_결제는_환불불가_사유_ALREADY_CANCELLED() {
        Payment p = Payment.builder().id(1L).status(PaymentStatus.CANCELLED).build();
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(p));

        RefundEligibilityResponseDto result = service.checkRefundEligibility(1L);

        assertThat(result.isRefundable()).isFalse();
        assertThat(result.getReason()).isEqualTo("ALREADY_CANCELLED");
    }

    @Test
    void 구독_주문은_UNSUPPORTED_ORDER_TYPE() {
        Order o = order(1L, user(1L), 30000, OrderStatus.PAID, OrderType.SUBSCRIPTION);
        Payment p = Payment.builder().id(1L).order(o).status(PaymentStatus.PAID).createdAt(LocalDateTime.now()).build();
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(p));

        RefundEligibilityResponseDto result = service.checkRefundEligibility(1L);

        assertThat(result.isRefundable()).isFalse();
        assertThat(result.getReason()).isEqualTo("UNSUPPORTED_ORDER_TYPE");
    }

    @Test
    void 환불기한_24시간_지나면_REFUND_DEADLINE_EXCEEDED() {
        Order o = order(1L, user(1L), 30000, OrderStatus.PAID, OrderType.GENERAL);
        Payment p = Payment.builder().id(1L).order(o).status(PaymentStatus.PAID)
                .createdAt(LocalDateTime.now().minusHours(25)).build();
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(p));

        RefundEligibilityResponseDto result = service.checkRefundEligibility(1L);

        assertThat(result.isRefundable()).isFalse();
        assertThat(result.getReason()).isEqualTo("REFUND_DEADLINE_EXCEEDED");
    }

    @Test
    void 이미_사용한_클래스면_PASS_ALREADY_USED() {
        Order o = order(1L, user(1L), 30000, OrderStatus.PAID, OrderType.GENERAL);
        Payment p = Payment.builder().id(1L).order(o).status(PaymentStatus.PAID)
                .createdAt(LocalDateTime.now()).build();
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(p));
        when(classEnrollmentService.isEnrollmentUsed(1L)).thenReturn(true);

        RefundEligibilityResponseDto result = service.checkRefundEligibility(1L);

        assertThat(result.isRefundable()).isFalse();
        assertThat(result.getReason()).isEqualTo("PASS_ALREADY_USED");
    }

    @Test
    void 조건_다_통과하면_환불가능() {
        Order o = order(1L, user(1L), 30000, OrderStatus.PAID, OrderType.GENERAL);
        Payment p = Payment.builder().id(1L).order(o).status(PaymentStatus.PAID)
                .createdAt(LocalDateTime.now()).build();
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(p));
        when(classEnrollmentService.isEnrollmentUsed(1L)).thenReturn(false);

        RefundEligibilityResponseDto result = service.checkRefundEligibility(1L);

        assertThat(result.isRefundable()).isTrue();
    }

    // ===== cancelPayment =====

    @Test
    void 본인_결제가_아니면_취소_불가() {
        Order o = order(1L, user(99L), 30000, OrderStatus.PAID, OrderType.GENERAL);
        Payment p = Payment.builder().id(1L).order(o).status(PaymentStatus.PAID).build();
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(p));

        assertThatThrownBy(() -> service.cancelPayment(1L, 1L, new PaymentCancelRequestDto("사유")))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getCustomError()).isEqualTo(CustomError.AUTH_UNAUTHORIZED));

        verifyNoInteractions(portOnePaymentClient);
    }

    @Test
    void 환불불가_조건이면_CANNOT_REFUND() {
        Order o = order(1L, user(1L), 30000, OrderStatus.PAID, OrderType.SUBSCRIPTION); // UNSUPPORTED_ORDER_TYPE 유도
        Payment p = Payment.builder().id(1L).order(o).status(PaymentStatus.PAID).createdAt(LocalDateTime.now()).build();
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(p));

        assertThatThrownBy(() -> service.cancelPayment(1L, 1L, new PaymentCancelRequestDto("사유")))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getCustomError()).isEqualTo(CustomError.CANNOT_REFUND));

        verifyNoInteractions(portOnePaymentClient);
    }

    // ===== refundForHostCancelledClass =====

    @Test
    void 이미_취소된_결제는_다시_처리하지_않고_조용히_리턴() {
        Order o = order(1L, user(1L), 30000, OrderStatus.CANCELLED, OrderType.GENERAL);
        Payment p = Payment.builder().id(1L).order(o).status(PaymentStatus.CANCELLED).build();
        when(paymentRepository.findByOrderId(1L)).thenReturn(Optional.of(p));

        service.refundForHostCancelledClass(1L, "호스트 취소");

        verifyNoInteractions(portOnePaymentClient);
    }

    @Test
    void 구독주문이면_스킵한다() {
        Order o = order(1L, user(1L), 30000, OrderStatus.PAID, OrderType.SUBSCRIPTION);
        Payment p = Payment.builder().id(1L).order(o).status(PaymentStatus.PAID).build();
        when(paymentRepository.findByOrderId(1L)).thenReturn(Optional.of(p));

        service.refundForHostCancelledClass(1L, "호스트 취소");

        verifyNoInteractions(portOnePaymentClient);
    }

    @Test
    void 호스트_취소_환불은_24시간_지났어도_환불된다() {
        // checkRefundEligibility를 아예 안 거치므로, 24시간이 지났어도 통과해야 한다.
        Order o = order(1L, user(1L), 30000, OrderStatus.PAID, OrderType.GENERAL);
        Payment p = Payment.builder().id(1L).order(o).portonePaymentId("pay_1").status(PaymentStatus.PAID)
                .createdAt(LocalDateTime.now().minusDays(10)).build();
        when(paymentRepository.findByOrderId(1L)).thenReturn(Optional.of(p));

        service.refundForHostCancelledClass(1L, "호스트 취소");

        verify(portOnePaymentClient).cancelPayment("pay_1", "호스트 취소");
        assertThat(p.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
        assertThat(o.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);
        verifyNoInteractions(classEnrollmentService); // ClassEnrollment는 여기서 안 건드림
    }
}
