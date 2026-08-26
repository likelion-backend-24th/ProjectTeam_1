package com.team1.cityfarm.service;

import com.team1.cityfarm.dto.PassResponseDto;
import com.team1.cityfarm.dto.SubscriptionPassUsageResponseDto;
import com.team1.cityfarm.dto.SubscriptionResponseDto;
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
import static org.mockito.Mockito.when;

/**
 * 3순위 — 단순 조회성 메서드 테스트: getMyActiveSubscription, getSubscriptionPass, getPassUsages.
 */
@ExtendWith(MockitoExtension.class)
class SubscriptionServiceQueryTest {

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

    private Subscription subscription(Long id, User u) {
        return Subscription.builder().id(id).user(u).planType(SubscriptionPlanType.BASIC)
                .status(SubscriptionStatus.ACTIVE).currentPeriodStart(LocalDateTime.now())
                .currentPeriodEnd(LocalDateTime.now().plusMonths(1)).cancelAtPeriodEnd(false).build();
    }

    // ===== getMyActiveSubscription =====

    @Test
    void 활성구독_없으면_예외() {
        when(subscriptionRepository.findByUserIdAndStatus(1L, SubscriptionStatus.ACTIVE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getMyActiveSubscription(1L))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getCustomError())
                        .isEqualTo(CustomError.SUBSCRIPTION_NOT_FOUND));
    }

    @Test
    void 활성구독_있으면_DTO로_변환해_반환() {
        User u = user(1L);
        Subscription sub = subscription(1L, u);
        when(subscriptionRepository.findByUserIdAndStatus(1L, SubscriptionStatus.ACTIVE)).thenReturn(Optional.of(sub));

        SubscriptionResponseDto result = service.getMyActiveSubscription(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getUserId()).isEqualTo(1L);
        assertThat(result.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    }

    // ===== getSubscriptionPass =====

    @Test
    void 활성_수강권이_없으면_예외() {
        when(subscriptionPassRepository.findBySubscriptionIdAndStatus(1L, PassStatus.ACTIVE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getSubscriptionPass(1L, 1L))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getCustomError())
                        .isEqualTo(CustomError.SUBSCRIPTION_PASS_NOT_FOUND));
    }

    @Test
    void 본인_수강권이_아니면_예외() {
        User owner = user(1L);
        Subscription sub = subscription(1L, owner);
        SubscriptionPass pass = SubscriptionPass.builder().id(1L).subscription(sub)
                .totalCount(3).remainingCount(3).status(PassStatus.ACTIVE).build();
        when(subscriptionPassRepository.findBySubscriptionIdAndStatus(1L, PassStatus.ACTIVE)).thenReturn(Optional.of(pass));

        assertThatThrownBy(() -> service.getSubscriptionPass(99L, 1L)) // 다른 유저 id로 조회
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getCustomError())
                        .isEqualTo(CustomError.AUTH_UNAUTHORIZED));
    }

    @Test
    void 본인_수강권이면_정상_반환() {
        User owner = user(1L);
        Subscription sub = subscription(1L, owner);
        SubscriptionPass pass = SubscriptionPass.builder().id(1L).subscription(sub)
                .totalCount(3).remainingCount(2).status(PassStatus.ACTIVE).build();
        when(subscriptionPassRepository.findBySubscriptionIdAndStatus(1L, PassStatus.ACTIVE)).thenReturn(Optional.of(pass));

        PassResponseDto result = service.getSubscriptionPass(1L, 1L);

        assertThat(result.getRemainingCount()).isEqualTo(2);
    }

    // ===== getPassUsages =====

    @Test
    void 수강권이_없으면_예외() {
        when(subscriptionPassRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getPassUsages(1L, 1L))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getCustomError())
                        .isEqualTo(CustomError.SUBSCRIPTION_PASS_NOT_FOUND));
    }

    @Test
    void 본인_수강권이_아니면_AUTH_UNAUTHORIZED() {
        User owner = user(1L);
        Subscription sub = subscription(1L, owner);
        SubscriptionPass pass = SubscriptionPass.builder().id(1L).subscription(sub)
                .totalCount(3).remainingCount(3).status(PassStatus.ACTIVE).build();
        when(subscriptionPassRepository.findById(1L)).thenReturn(Optional.of(pass));

        assertThatThrownBy(() -> service.getPassUsages(99L, 1L))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getCustomError())
                        .isEqualTo(CustomError.AUTH_UNAUTHORIZED));
    }

    @Test
    void 본인_수강권이면_사용내역_목록_반환() {
        User owner = user(1L);
        Subscription sub = subscription(1L, owner);
        SubscriptionPass pass = SubscriptionPass.builder().id(1L).subscription(sub)
                .totalCount(3).remainingCount(2).status(PassStatus.ACTIVE).build();
        OneDayClass clazz = OneDayClass.builder().id(5L).title("클래스").build();
        ClassEnrollment enrollment = ClassEnrollment.builder().id(3L).oneDayClass(clazz).build();
        SubscriptionPassUsage usage = SubscriptionPassUsage.builder()
                .subscriptionPass(pass).enrollment(enrollment).usedAt(LocalDateTime.now()).build();

        when(subscriptionPassRepository.findById(1L)).thenReturn(Optional.of(pass));
        when(subscriptionPassUsageRepository.findBySubscriptionPassIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(usage));

        List<SubscriptionPassUsageResponseDto> result = service.getPassUsages(1L, 1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getClassId()).isEqualTo(5L);
    }
}
