package com.team1.cityfarm.service;

import com.team1.cityfarm.entity.*;
import com.team1.cityfarm.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * SubscriptionService.migrateActiveScheduleToNewBillingKey 단위 테스트.
 * 카드 변경 시 이전 빌링키에 걸린 다음 회차 예약을 새 빌링키로 옮기는 로직 —
 * BillingKeyService.confirmIssuance가 이전 키를 지우기 전에 반드시 호출해야 하는 지점.
 */
@ExtendWith(MockitoExtension.class)
class SubscriptionServiceMigrationTest {

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
    @Mock private org.springframework.transaction.PlatformTransactionManager transactionManager;

    private SubscriptionService service;

    @BeforeEach
    void setUp() {
        service = new SubscriptionService(
                subscriptionRepository, userRepository, subscriptionPassRepository,
                subscriptionPassUsageRepository, subscriptionScheduleRepository, billingKeyRepository,
                orderRepository, paymentRepository, portonePaymentClient, classEnrollmentService,
                settlementService, transactionManager
        );
    }

    private User user(Long id) {
        return User.builder().id(id).email("u@test.com").password("pw").nickname("nick").build();
    }

    private Subscription subscription(User u) {
        return Subscription.builder().id(1L).user(u).planType(SubscriptionPlanType.BASIC)
                .status(SubscriptionStatus.ACTIVE).currentPeriodStart(LocalDateTime.now())
                .currentPeriodEnd(LocalDateTime.now().plusMonths(1)).cancelAtPeriodEnd(false).build();
    }

    @Test
    void 활성_구독이_없으면_아무것도_안_한다() {
        when(subscriptionRepository.findByUserIdAndStatus(1L, SubscriptionStatus.ACTIVE)).thenReturn(Optional.empty());
        BillingKey newKey = BillingKey.builder().id(2L).billingKeyEncrypted("bk_new").status(BillingKeyStatus.ACTIVE).build();

        service.migrateActiveScheduleToNewBillingKey(1L, newKey);

        verifyNoInteractions(portonePaymentClient, subscriptionScheduleRepository);
    }

    @Test
    void SCHEDULED_예약이_없으면_아무것도_안_한다() {
        User u = user(1L);
        Subscription sub = subscription(u);
        when(subscriptionRepository.findByUserIdAndStatus(1L, SubscriptionStatus.ACTIVE)).thenReturn(Optional.of(sub));
        when(subscriptionScheduleRepository.findBySubscriptionIdAndStatus(1L, ScheduleStatus.SCHEDULED)).thenReturn(Optional.empty());
        BillingKey newKey = BillingKey.builder().id(2L).billingKeyEncrypted("bk_new").status(BillingKeyStatus.ACTIVE).build();

        service.migrateActiveScheduleToNewBillingKey(1L, newKey);

        verifyNoInteractions(portonePaymentClient);
        verify(subscriptionScheduleRepository, never()).save(any());
    }

    @Test
    void 예약이_있으면_기존예약_취소하고_새_빌링키로_같은_회차를_재예약한다() {
        User u = user(1L);
        Subscription sub = subscription(u);
        LocalDateTime scheduledAt = LocalDateTime.now().plusDays(10);
        SubscriptionSchedule oldSchedule = SubscriptionSchedule.builder()
                .id(9L).subscription(sub).round(3).scheduledAt(scheduledAt).amount(30000)
                .status(ScheduleStatus.SCHEDULED).portoneScheduleId("sch_old").paymentId("pay_old").build();
        BillingKey newKey = BillingKey.builder().id(2L).billingKeyEncrypted("bk_new").status(BillingKeyStatus.ACTIVE).build();

        when(subscriptionRepository.findByUserIdAndStatus(1L, SubscriptionStatus.ACTIVE)).thenReturn(Optional.of(sub));
        when(subscriptionScheduleRepository.findBySubscriptionIdAndStatus(1L, ScheduleStatus.SCHEDULED)).thenReturn(Optional.of(oldSchedule));

        service.migrateActiveScheduleToNewBillingKey(1L, newKey);

        verify(portonePaymentClient).cancelSchedule("sch_old");
        assertThat(oldSchedule.getStatus()).isEqualTo(ScheduleStatus.CANCELLED);

        // 같은 회차(round=3)/같은 금액/같은 예정일로, 새 빌링키를 써서 다시 예약해야 함
        verify(portonePaymentClient).schedulePayment(eq("bk_new"), any(), any(), eq(30000), eq(1L), eq(scheduledAt));

        org.mockito.ArgumentCaptor<SubscriptionSchedule> captor = org.mockito.ArgumentCaptor.forClass(SubscriptionSchedule.class);
        verify(subscriptionScheduleRepository).save(captor.capture());
        SubscriptionSchedule newSchedule = captor.getValue();
        assertThat(newSchedule.getRound()).isEqualTo(3);
        assertThat(newSchedule.getBillingKeyId()).isEqualTo(2L);
        assertThat(newSchedule.getStatus()).isEqualTo(ScheduleStatus.SCHEDULED);
    }

    @Test
    void 취소_대상_스케줄에_portoneScheduleId가_없으면_취소_API는_안_부르고_재예약만_한다() {
        User u = user(1L);
        Subscription sub = subscription(u);
        SubscriptionSchedule oldSchedule = SubscriptionSchedule.builder()
                .id(9L).subscription(sub).round(3).scheduledAt(LocalDateTime.now().plusDays(10)).amount(30000)
                .status(ScheduleStatus.SCHEDULED).portoneScheduleId(null).paymentId("pay_old").build();
        BillingKey newKey = BillingKey.builder().id(2L).billingKeyEncrypted("bk_new").status(BillingKeyStatus.ACTIVE).build();

        when(subscriptionRepository.findByUserIdAndStatus(1L, SubscriptionStatus.ACTIVE)).thenReturn(Optional.of(sub));
        when(subscriptionScheduleRepository.findBySubscriptionIdAndStatus(1L, ScheduleStatus.SCHEDULED)).thenReturn(Optional.of(oldSchedule));

        service.migrateActiveScheduleToNewBillingKey(1L, newKey);

        verify(portonePaymentClient, never()).cancelSchedule(any());
        verify(portonePaymentClient).schedulePayment(eq("bk_new"), any(), any(), eq(30000), eq(1L), any());
    }
}
