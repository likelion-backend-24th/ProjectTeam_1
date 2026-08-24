package com.team1.cityfarm.service;

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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * cancelSubscriptionAtPeriodEnd / expireCancelledSubscriptions / hasActiveScheduledPayment
 * 단위 테스트 (2순위 — 분기는 단순하지만 최근에 만든 로직들).
 */
@ExtendWith(MockitoExtension.class)
class SubscriptionServiceLifecycleTest {

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

    // ===== cancelSubscriptionAtPeriodEnd =====

    @Test
    void 구독이_없으면_예외() {
        when(subscriptionRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancelSubscriptionAtPeriodEnd(1L, 1L))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getCustomError())
                        .isEqualTo(CustomError.SUBSCRIPTION_NOT_FOUND));
    }

    @Test
    void 본인_구독이_아니면_예외() {
        Subscription sub = subscription(1L, user(99L), false);
        when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(sub));

        assertThatThrownBy(() -> service.cancelSubscriptionAtPeriodEnd(1L, 1L))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getCustomError())
                        .isEqualTo(CustomError.AUTH_UNAUTHORIZED));
    }

    @Test
    void 이미_해지예약됐으면_멱등하게_아무것도_안_한다() {
        Subscription sub = subscription(1L, user(1L), true);
        when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(sub));

        service.cancelSubscriptionAtPeriodEnd(1L, 1L);

        verifyNoInteractions(subscriptionScheduleRepository, portonePaymentClient);
    }

    @Test
    void 예약된_스케줄이_없으면_플래그만_세운다() {
        Subscription sub = subscription(1L, user(1L), false);
        when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(sub));
        when(subscriptionScheduleRepository.findBySubscriptionIdAndStatus(1L, ScheduleStatus.SCHEDULED))
                .thenReturn(Optional.empty());

        service.cancelSubscriptionAtPeriodEnd(1L, 1L);

        assertThat(sub.isCancelAtPeriodEnd()).isTrue();
        verifyNoInteractions(portonePaymentClient);
    }

    @Test
    void 예약된_스케줄이_있으면_PortOne_취소하고_스케줄도_CANCELLED로_바꾼다() {
        Subscription sub = subscription(1L, user(1L), false);
        SubscriptionSchedule sch = SubscriptionSchedule.builder().id(9L).subscription(sub).round(2)
                .scheduledAt(LocalDateTime.now()).amount(30000).status(ScheduleStatus.SCHEDULED)
                .portoneScheduleId("sch_1").build();

        when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(sub));
        when(subscriptionScheduleRepository.findBySubscriptionIdAndStatus(1L, ScheduleStatus.SCHEDULED))
                .thenReturn(Optional.of(sch));

        service.cancelSubscriptionAtPeriodEnd(1L, 1L);

        verify(portonePaymentClient).cancelSchedule("sch_1");
        assertThat(sch.getStatus()).isEqualTo(ScheduleStatus.CANCELLED);
        assertThat(sub.isCancelAtPeriodEnd()).isTrue();
    }

    @Test
    void portoneScheduleId가_없으면_PortOne_취소_없이_상태만_바꾼다() {
        Subscription sub = subscription(1L, user(1L), false);
        SubscriptionSchedule sch = SubscriptionSchedule.builder().id(9L).subscription(sub).round(2)
                .scheduledAt(LocalDateTime.now()).amount(30000).status(ScheduleStatus.SCHEDULED)
                .portoneScheduleId(null).build();

        when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(sub));
        when(subscriptionScheduleRepository.findBySubscriptionIdAndStatus(1L, ScheduleStatus.SCHEDULED))
                .thenReturn(Optional.of(sch));

        service.cancelSubscriptionAtPeriodEnd(1L, 1L);

        verify(portonePaymentClient, never()).cancelSchedule(any());
        assertThat(sch.getStatus()).isEqualTo(ScheduleStatus.CANCELLED);
    }

    // ===== expireCancelledSubscriptions =====

    @Test
    void 대상이_없으면_0을_반환한다() {
        when(subscriptionRepository.findByStatusAndCancelAtPeriodEndTrueAndCurrentPeriodEndBefore(
                eq(SubscriptionStatus.ACTIVE), any())).thenReturn(List.of());

        int count = service.expireCancelledSubscriptions();

        assertThat(count).isZero();
    }

    @Test
    void 대상_전부_CANCELLED로_바뀌고_개수를_반환한다() {
        Subscription s1 = subscription(1L, user(1L), true);
        Subscription s2 = subscription(2L, user(2L), true);
        when(subscriptionRepository.findByStatusAndCancelAtPeriodEndTrueAndCurrentPeriodEndBefore(
                eq(SubscriptionStatus.ACTIVE), any())).thenReturn(List.of(s1, s2));

        int count = service.expireCancelledSubscriptions();

        assertThat(count).isEqualTo(2);
        assertThat(s1.getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
        assertThat(s1.getCancelledAt()).isNotNull();
        assertThat(s2.getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
        assertThat(s2.getCancelledAt()).isNotNull();
    }

    // ===== hasActiveScheduledPayment =====

    @Test
    void 활성구독이_없으면_false() {
        when(subscriptionRepository.findByUserIdAndStatus(1L, SubscriptionStatus.ACTIVE)).thenReturn(Optional.empty());

        assertThat(service.hasActiveScheduledPayment(1L)).isFalse();
    }

    @Test
    void 활성구독은_있지만_예약이_없으면_false() {
        Subscription sub = subscription(1L, user(1L), false);
        when(subscriptionRepository.findByUserIdAndStatus(1L, SubscriptionStatus.ACTIVE)).thenReturn(Optional.of(sub));
        when(subscriptionScheduleRepository.findBySubscriptionIdAndStatus(1L, ScheduleStatus.SCHEDULED))
                .thenReturn(Optional.empty());

        assertThat(service.hasActiveScheduledPayment(1L)).isFalse();
    }

    @Test
    void 활성구독에_예약이_있으면_true() {
        Subscription sub = subscription(1L, user(1L), false);
        SubscriptionSchedule sch = SubscriptionSchedule.builder().id(9L).subscription(sub).round(2)
                .scheduledAt(LocalDateTime.now()).amount(30000).status(ScheduleStatus.SCHEDULED).build();
        when(subscriptionRepository.findByUserIdAndStatus(1L, SubscriptionStatus.ACTIVE)).thenReturn(Optional.of(sub));
        when(subscriptionScheduleRepository.findBySubscriptionIdAndStatus(1L, ScheduleStatus.SCHEDULED))
                .thenReturn(Optional.of(sch));

        assertThat(service.hasActiveScheduledPayment(1L)).isTrue();
    }
}
