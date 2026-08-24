package com.team1.cityfarm.service;

import com.team1.cityfarm.dto.SubscriptionCancelResponseDto;
import com.team1.cityfarm.dto.SubscriptionCancelResultType;
import com.team1.cityfarm.entity.*;
import com.team1.cityfarm.global.exception.CustomError;
import com.team1.cityfarm.global.exception.CustomException;
import com.team1.cityfarm.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * requestCancellation (구독 해지/환불 요청) 단위 테스트.
 * 정책: 결제 승인 시각 기준 24시간 이내 + 이번 회차 수강권 미사용이면 전액 환불 후 즉시 해지,
 * 그 외에는 기존과 동일하게 해지 예약(cancelAtPeriodEnd)으로 처리된다.
 */
@ExtendWith(MockitoExtension.class)
class SubscriptionServiceRefundTest {

    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private UserRepository userRepository;
    @Mock private SubscriptionPassRepository subscriptionPassRepository;
    @Mock private SubscriptionPassUsageRepository subscriptionPassUsageRepository;
    @Mock private SubscriptionScheduleRepository subscriptionScheduleRepository;
    @Mock private BillingKeyRepository billingKeyRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private com.team1.cityfarm.portone.PortonePaymentClient portonePaymentClient;
    @Mock private ClassEnrollmentService classEnrollmentService;
    @Mock private SettlementService settlementService;

    private SubscriptionService service;

    @BeforeEach
    void setUp() {
        service = new SubscriptionService(
                subscriptionRepository, userRepository, subscriptionPassRepository,
                subscriptionPassUsageRepository, subscriptionScheduleRepository, billingKeyRepository,
                orderRepository, paymentRepository, portonePaymentClient, classEnrollmentService,
                settlementService
        );
    }

    private User user(Long id) {
        return User.builder().id(id).email("u@test.com").password("pw").nickname("nick").build();
    }

    private Subscription subscription(Long id, User u, boolean cancelAtPeriodEnd) {
        return Subscription.builder().id(id).user(u).planType(SubscriptionPlanType.BASIC)
                .status(SubscriptionStatus.ACTIVE).currentPeriodStart(LocalDateTime.now())
                .currentPeriodEnd(LocalDateTime.now().plusMonths(1)).cancelAtPeriodEnd(cancelAtPeriodEnd).build();
    }

    private SubscriptionPass pass(Long id, Subscription sub) {
        return SubscriptionPass.builder().id(id).subscription(sub)
                .totalCount(3).remainingCount(3).status(PassStatus.ACTIVE).build();
    }

    private Order order(Long id, Subscription sub) {
        return Order.builder().id(id).user(sub.getUser()).subscription(sub)
                .amount(30000).merchantOrderId("BE24-CITYFARM-TEST" + id)
                .orderType(OrderType.SUBSCRIPTION).orderStatus(OrderStatus.PAID).build();
    }

    private Payment payment(Long id, Order order, LocalDateTime approvedAt) {
        return Payment.builder().id(id).order(order).portonePaymentId("pay_" + id)
                .amount(order.getAmount()).status(PaymentStatus.PAID).approvedAt(approvedAt).build();
    }

    // ===== 공통 검증 =====

    @Test
    void 구독이_없으면_예외() {
        when(subscriptionRepository.findByIdForUpdate(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.requestCancellation(1L, 1L))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getCustomError())
                        .isEqualTo(CustomError.SUBSCRIPTION_NOT_FOUND));
    }

    @Test
    void 본인_구독이_아니면_예외() {
        Subscription sub = subscription(1L, user(99L), false);
        when(subscriptionRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sub));

        assertThatThrownBy(() -> service.requestCancellation(1L, 1L))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getCustomError())
                        .isEqualTo(CustomError.AUTH_UNAUTHORIZED));
    }

    // ===== 환불 자격 O =====

    @Test
    void 결제_24시간_이내_미사용이면_전액환불_후_즉시해지() {
        User u = user(1L);
        Subscription sub = subscription(1L, u, false);
        SubscriptionPass pass = pass(10L, sub);
        Order order = order(100L, sub);
        Payment payment = payment(1000L, order, LocalDateTime.now().minusHours(1));

        when(subscriptionRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sub));
        when(subscriptionPassRepository.findBySubscriptionIdAndStatusForUpdate(1L, PassStatus.ACTIVE))
                .thenReturn(Optional.of(pass));
        when(subscriptionPassUsageRepository.existsBySubscriptionPassId(10L)).thenReturn(false);
        when(orderRepository.findTopBySubscriptionIdOrderByCreatedAtDesc(1L)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(100L)).thenReturn(Optional.of(payment));
        when(subscriptionScheduleRepository.findBySubscriptionIdAndStatus(1L, ScheduleStatus.SCHEDULED))
                .thenReturn(Optional.empty());

        SubscriptionCancelResponseDto result = service.requestCancellation(1L, 1L);

        assertThat(result.getResultType()).isEqualTo(SubscriptionCancelResultType.REFUNDED);
        assertThat(result.getRefundedAmount()).isEqualTo(30000);
        verify(portonePaymentClient).cancelPayment(eq("pay_1000"), any());
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
        assertThat(sub.getCancelledAt()).isNotNull();
        assertThat(pass.getStatus()).isEqualTo(PassStatus.EXPIRED);
        // 해지 예약 플로우(cancelSubscriptionAtPeriodEnd)는 타지 않는다
        assertThat(sub.isCancelAtPeriodEnd()).isFalse();
    }

    @Test
    void 환불시_예약된_다음_회차_결제도_함께_취소한다() {
        User u = user(1L);
        Subscription sub = subscription(1L, u, false);
        SubscriptionPass pass = pass(10L, sub);
        Order order = order(100L, sub);
        Payment payment = payment(1000L, order, LocalDateTime.now().minusHours(1));
        SubscriptionSchedule schedule = SubscriptionSchedule.builder().id(9L).subscription(sub).round(2)
                .scheduledAt(LocalDateTime.now()).amount(30000).status(ScheduleStatus.SCHEDULED)
                .portoneScheduleId("sch_1").build();

        when(subscriptionRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sub));
        when(subscriptionPassRepository.findBySubscriptionIdAndStatusForUpdate(1L, PassStatus.ACTIVE))
                .thenReturn(Optional.of(pass));
        when(subscriptionPassUsageRepository.existsBySubscriptionPassId(10L)).thenReturn(false);
        when(orderRepository.findTopBySubscriptionIdOrderByCreatedAtDesc(1L)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(100L)).thenReturn(Optional.of(payment));
        when(subscriptionScheduleRepository.findBySubscriptionIdAndStatus(1L, ScheduleStatus.SCHEDULED))
                .thenReturn(Optional.of(schedule));

        service.requestCancellation(1L, 1L);

        verify(portonePaymentClient).cancelSchedule("sch_1");
        assertThat(schedule.getStatus()).isEqualTo(ScheduleStatus.CANCELLED);
    }

    // ===== 환불 자격 X → 기존 해지 예약 폴백 =====

    @Test
    void 수강권을_이미_사용했으면_환불없이_해지예약() {
        User u = user(1L);
        Subscription sub = subscription(1L, u, false);
        SubscriptionPass pass = pass(10L, sub);

        when(subscriptionRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sub));
        when(subscriptionPassRepository.findBySubscriptionIdAndStatusForUpdate(1L, PassStatus.ACTIVE))
                .thenReturn(Optional.of(pass));
        when(subscriptionPassUsageRepository.existsBySubscriptionPassId(10L)).thenReturn(true);
        // 해지예약 폴백 경로(cancelSubscriptionAtPeriodEnd)가 다시 조회함
        when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(sub));
        when(subscriptionScheduleRepository.findBySubscriptionIdAndStatus(1L, ScheduleStatus.SCHEDULED))
                .thenReturn(Optional.empty());

        SubscriptionCancelResponseDto result = service.requestCancellation(1L, 1L);

        assertThat(result.getResultType()).isEqualTo(SubscriptionCancelResultType.SCHEDULED_CANCEL);
        assertThat(sub.isCancelAtPeriodEnd()).isTrue();
        assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        verify(portonePaymentClient, never()).cancelPayment(any(), any());
    }

    @Test
    void 결제_24시간이_지났으면_환불없이_해지예약() {
        User u = user(1L);
        Subscription sub = subscription(1L, u, false);
        SubscriptionPass pass = pass(10L, sub);
        Order order = order(100L, sub);
        Payment payment = payment(1000L, order, LocalDateTime.now().minusHours(25));

        when(subscriptionRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sub));
        when(subscriptionPassRepository.findBySubscriptionIdAndStatusForUpdate(1L, PassStatus.ACTIVE))
                .thenReturn(Optional.of(pass));
        when(subscriptionPassUsageRepository.existsBySubscriptionPassId(10L)).thenReturn(false);
        when(orderRepository.findTopBySubscriptionIdOrderByCreatedAtDesc(1L)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(100L)).thenReturn(Optional.of(payment));
        when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(sub));
        when(subscriptionScheduleRepository.findBySubscriptionIdAndStatus(1L, ScheduleStatus.SCHEDULED))
                .thenReturn(Optional.empty());

        SubscriptionCancelResponseDto result = service.requestCancellation(1L, 1L);

        assertThat(result.getResultType()).isEqualTo(SubscriptionCancelResultType.SCHEDULED_CANCEL);
        assertThat(sub.isCancelAtPeriodEnd()).isTrue();
        verify(portonePaymentClient, never()).cancelPayment(any(), any());
    }

    @Test
    void 활성_수강권이_없으면_환불없이_해지예약으로_안전하게_폴백() {
        User u = user(1L);
        Subscription sub = subscription(1L, u, false);

        when(subscriptionRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sub));
        when(subscriptionPassRepository.findBySubscriptionIdAndStatusForUpdate(1L, PassStatus.ACTIVE))
                .thenReturn(Optional.empty());
        when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(sub));
        when(subscriptionScheduleRepository.findBySubscriptionIdAndStatus(1L, ScheduleStatus.SCHEDULED))
                .thenReturn(Optional.empty());

        SubscriptionCancelResponseDto result = service.requestCancellation(1L, 1L);

        assertThat(result.getResultType()).isEqualTo(SubscriptionCancelResultType.SCHEDULED_CANCEL);
        verifyNoInteractions(orderRepository, paymentRepository);
    }

    @Test
    void 이미_해지예약된_구독이면_환불판정_없이_그대로_SCHEDULED_CANCEL() {
        Subscription sub = subscription(1L, user(1L), true);
        when(subscriptionRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sub));
        when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(sub));

        SubscriptionCancelResponseDto result = service.requestCancellation(1L, 1L);

        assertThat(result.getResultType()).isEqualTo(SubscriptionCancelResultType.SCHEDULED_CANCEL);
        verifyNoInteractions(subscriptionPassRepository, orderRepository, paymentRepository, portonePaymentClient);
    }
}
