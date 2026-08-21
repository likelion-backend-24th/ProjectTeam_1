package com.team1.cityfarm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team1.cityfarm.entity.*;
import com.team1.cityfarm.portone.PortonePaymentResponseDto;
import com.team1.cityfarm.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * SubscriptionService.renewSubscription 단위 테스트 (DB/Spring 컨텍스트 없이 Mockito만 사용).
 * 이 메서드는 PortOne 웹훅으로만 트리거되는, 이 프로젝트에서 가장 분기가 복잡한 메서드라
 * 회귀 방지 가치가 가장 크다고 판단해 1순위로 다룬다.
 */
@ExtendWith(MockitoExtension.class)
class SubscriptionServiceRenewalTest {

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
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        service = new SubscriptionService(
                subscriptionRepository, userRepository, subscriptionPassRepository,
                subscriptionPassUsageRepository, subscriptionScheduleRepository, billingKeyRepository,
                orderRepository, paymentRepository, portonePaymentClient, classEnrollmentService,
                settlementService
        );
    }

    private PortonePaymentResponseDto paidResponse(int amount) throws Exception {
        String json = """
                {"id":"pay_x","status":"PAID","paidAt":"2026-08-20T00:00:00Z",
                 "amount":{"total":%d,"paid":%d},"method":{"type":"Card","provider":"CARD"}}
                """.formatted(amount, amount);
        return objectMapper.readValue(json, PortonePaymentResponseDto.class);
    }

    private User user(Long id) {
        return User.builder().id(id).email("u@test.com").password("pw").nickname("nick").build();
    }

    private Subscription subscription(User u, boolean cancelAtPeriodEnd) {
        return Subscription.builder()
                .id(1L)
                .user(u)
                .planType(SubscriptionPlanType.BASIC)
                .status(SubscriptionStatus.ACTIVE)
                .currentPeriodStart(LocalDateTime.now().minusMonths(1))
                .currentPeriodEnd(LocalDateTime.now())
                .cancelAtPeriodEnd(cancelAtPeriodEnd)
                .build();
    }

    private SubscriptionSchedule schedule(Subscription sub, ScheduleStatus status, int amount) {
        return SubscriptionSchedule.builder()
                .id(10L)
                .subscription(sub)
                .round(2)
                .scheduledAt(LocalDateTime.now())
                .amount(amount)
                .status(status)
                .paymentId("pay_x")
                .portoneScheduleId("sch_x")
                .build();
    }

    @Test
    void 매핑되는_스케줄이_없으면_false를_반환하고_아무_것도_안_한다() {
        when(subscriptionScheduleRepository.findByPaymentId("no_match")).thenReturn(Optional.empty());

        boolean result = service.renewSubscription("no_match", null);

        assertThat(result).isFalse();
        verifyNoInteractions(orderRepository, paymentRepository);
    }

    @Test
    void 이미_PAID된_회차면_멱등하게_true만_반환한다() throws Exception {
        User u = user(1L);
        Subscription sub = subscription(u, false);
        SubscriptionSchedule sch = schedule(sub, ScheduleStatus.PAID, 30000);
        when(subscriptionScheduleRepository.findByPaymentId("pay_x")).thenReturn(Optional.of(sch));

        boolean result = service.renewSubscription("pay_x", paidResponse(30000));

        assertThat(result).isTrue();
        verify(orderRepository, never()).save(any());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void 결제_금액이_스케줄_금액과_다르면_처리하지_않고_true만_반환한다() throws Exception {
        User u = user(1L);
        Subscription sub = subscription(u, false);
        SubscriptionSchedule sch = schedule(sub, ScheduleStatus.SCHEDULED, 30000);
        when(subscriptionScheduleRepository.findByPaymentId("pay_x")).thenReturn(Optional.of(sch));

        boolean result = service.renewSubscription("pay_x", paidResponse(9999)); // 금액 위변조 시도

        assertThat(result).isTrue();
        verify(orderRepository, never()).save(any());
        verify(paymentRepository, never()).save(any());
        assertThat(sch.getStatus()).isEqualTo(ScheduleStatus.SCHEDULED); // 상태도 안 바뀜
    }

    @Test
    void 정상_갱신_해지예약_없으면_다음_회차를_다시_예약한다() throws Exception {
        User u = user(1L);
        Subscription sub = subscription(u, false);
        SubscriptionSchedule sch = schedule(sub, ScheduleStatus.SCHEDULED, 30000);
        BillingKey activeKey = BillingKey.builder().id(5L).user(u)
                .billingKeyEncrypted("bk_new").status(BillingKeyStatus.ACTIVE).build();

        when(subscriptionScheduleRepository.findByPaymentId("pay_x")).thenReturn(Optional.of(sch));
        when(subscriptionPassRepository.findBySubscriptionIdAndStatus(1L, PassStatus.ACTIVE)).thenReturn(Optional.empty());
        when(billingKeyRepository.findByUserIdAndStatus(1L, BillingKeyStatus.ACTIVE)).thenReturn(Optional.of(activeKey));

        boolean result = service.renewSubscription("pay_x", paidResponse(30000));

        assertThat(result).isTrue();
        assertThat(sch.getStatus()).isEqualTo(ScheduleStatus.PAID);
        assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE); // CANCELLED로 안 바뀜
        verify(orderRepository).save(any(Order.class));
        verify(paymentRepository).save(any(Payment.class));
        verify(subscriptionPassRepository).save(any(SubscriptionPass.class)); // 새 회차 패스 발급
        // 다음 회차 예약 -> schedulePayment 호출됨 (createNextSchedule 내부)
        verify(portonePaymentClient).schedulePayment(eq("bk_new"), any(), any(), eq(30000), eq(1L), any());
        // 이번 회차 스케줄은 setter만 호출되고(실제 DB에선 dirty checking으로 반영), save()는
        // createNextSchedule 안에서 다음 회차용으로 딱 1번만 명시적으로 호출됨.
        verify(subscriptionScheduleRepository, times(1)).save(any(SubscriptionSchedule.class));
    }

    @Test
    void 해지예약된_구독은_이번_회차로_종료되고_다음_예약을_안_만든다() throws Exception {
        User u = user(1L);
        Subscription sub = subscription(u, true); // cancelAtPeriodEnd = true
        SubscriptionSchedule sch = schedule(sub, ScheduleStatus.SCHEDULED, 30000);

        when(subscriptionScheduleRepository.findByPaymentId("pay_x")).thenReturn(Optional.of(sch));
        when(subscriptionPassRepository.findBySubscriptionIdAndStatus(1L, PassStatus.ACTIVE)).thenReturn(Optional.empty());

        boolean result = service.renewSubscription("pay_x", paidResponse(30000));

        assertThat(result).isTrue();
        assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
        assertThat(sub.getCancelledAt()).isNotNull();
        verify(portonePaymentClient, never()).schedulePayment(any(), any(), any(), anyInt(), any(), any());
        verify(billingKeyRepository, never()).findByUserIdAndStatus(any(), any());
    }

    @Test
    void 빌링키가_없으면_로그만_남기고_다음_예약을_안_만든다() throws Exception {
        User u = user(1L);
        Subscription sub = subscription(u, false);
        SubscriptionSchedule sch = schedule(sub, ScheduleStatus.SCHEDULED, 30000);

        when(subscriptionScheduleRepository.findByPaymentId("pay_x")).thenReturn(Optional.of(sch));
        when(subscriptionPassRepository.findBySubscriptionIdAndStatus(1L, PassStatus.ACTIVE)).thenReturn(Optional.empty());
        when(billingKeyRepository.findByUserIdAndStatus(1L, BillingKeyStatus.ACTIVE)).thenReturn(Optional.empty());

        boolean result = service.renewSubscription("pay_x", paidResponse(30000));

        assertThat(result).isTrue();
        assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        verify(portonePaymentClient, never()).schedulePayment(any(), any(), any(), anyInt(), any(), any());
    }
}
