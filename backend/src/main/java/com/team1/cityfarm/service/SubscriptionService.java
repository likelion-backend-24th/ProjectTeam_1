package com.team1.cityfarm.service;

import com.team1.cityfarm.dto.MyEnrollmentResponseDto;
import com.team1.cityfarm.dto.PassResponseDto;
import com.team1.cityfarm.dto.SubscriptionCancelResponseDto;
import com.team1.cityfarm.dto.SubscriptionCancelResultType;
import com.team1.cityfarm.dto.SubscriptionCreateRequestDto;
import com.team1.cityfarm.dto.SubscriptionPassDto;
import com.team1.cityfarm.dto.SubscriptionPassUsageResponseDto;
import com.team1.cityfarm.dto.SubscriptionResponseDto;
import com.team1.cityfarm.entity.*;
import com.team1.cityfarm.global.exception.CustomError;
import com.team1.cityfarm.global.exception.CustomException;
import com.team1.cityfarm.portone.PortonePaymentClient;
import com.team1.cityfarm.portone.PortonePaymentResponseDto;
import com.team1.cityfarm.portone.PortonePaymentScheduleResponseDto;
import com.team1.cityfarm.repository.BillingKeyRepository;
import com.team1.cityfarm.repository.OrderRepository;
import com.team1.cityfarm.repository.PaymentRepository;
import com.team1.cityfarm.repository.SubscriptionPassRepository;
import com.team1.cityfarm.repository.SubscriptionPassUsageRepository;
import com.team1.cityfarm.repository.SubscriptionRepository;
import com.team1.cityfarm.repository.SubscriptionScheduleRepository;
import com.team1.cityfarm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionService {

    // 결제 승인 시각 기준 이 시간 이내 + 수강권 미사용이면 전액 환불 대상 (매 갱신 회차마다 적용)
    private static final int REFUND_ELIGIBLE_HOURS = 24;

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final SubscriptionPassRepository subscriptionPassRepository;
    private final SubscriptionPassUsageRepository subscriptionPassUsageRepository;
    private final SubscriptionScheduleRepository subscriptionScheduleRepository;
    private final BillingKeyRepository billingKeyRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final PortonePaymentClient portonePaymentClient;
    private final ClassEnrollmentService classEnrollmentService;
    private final SettlementService settlementService;
    private final PlatformTransactionManager transactionManager;

    /**
     * 1. 내 활성 구독 정보 조회
     */
    @Transactional(readOnly = true)
    public SubscriptionResponseDto getMyActiveSubscription(Long userId) {
        Subscription subscription = subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE)
                .orElseThrow(() -> new CustomException(CustomError.SUBSCRIPTION_NOT_FOUND));

        return SubscriptionResponseDto.from(subscription);
    }

    /**
     * 2. 정기 구독 신청 (빌링키 결제 및 수강권 발급)
     *
     * 카드 승인(payWithBillingKey)은 외부에 실제로 돈이 오가는 되돌릴 수 없는 호출이라,
     * 그 뒤의 DB 저장과 한 트랜잭션으로 묶으면 안 된다 — 승인 뒤에 일어나는 어떤 실패든(특히
     * 다음 회차 예약 실패) 트랜잭션을 롤백시켜 "카드는 결제됐는데 우리 DB엔 기록이 하나도
     * 없는" 상태를 만든다. 그래서 결제 성공 뒤 구독/수강권/주문/결제 저장까지만 별도
     * 트랜잭션(persistActivatedSubscription)으로 커밋을 확정하고, 그다음 회차 예약은
     * 이 트랜잭션 밖에서 별도로 시도해 실패해도 이미 확정된 결제 기록을 건드리지 않는다.
     */
    public SubscriptionResponseDto createSubscription(Long userId, SubscriptionCreateRequestDto request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(CustomError.USER_NOT_FOUND));

        // 이미 활성화된 구독이 있는지 검증
        if (subscriptionRepository.existsByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE)) {
            throw new CustomException(CustomError.DUPLICATE_SUBSCRIPTION);
        }

        // 프론트가 보낸 billingKeyId로 우리 DB의 BillingKey row를 직접 조회해 실제 PortOne 키 값을 꺼내 쓴다
        // (클라이언트가 PortOne 키 문자열을 직접 들고 다니지 않도록).
        BillingKey billingKeyEntity = billingKeyRepository.findById(request.getBillingKeyId())
                .orElseThrow(() -> new CustomException(CustomError.BILLING_KEY_NOT_FOUND));
        if (!billingKeyEntity.getUser().getId().equals(userId)) {
            throw new CustomException(CustomError.AUTH_UNAUTHORIZED);
        }
        String billingKey = billingKeyEntity.getBillingKeyEncrypted();

        // 요금제(PlanType)에 따른 결제 금액 설정
        int price = calculatePrice(request.getPlanType());
        String orderName = request.getPlanType().name() + " 정기구독";

        // 고유 결제 번호(paymentId) 생성
        String paymentId = "BE24-CITYFARM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // 포트원 빌링키 결제 요청 (실패 시 CustomException 발생, 이 시점엔 아직 아무것도 저장 안 했으므로 롤백할 것도 없음)
        PortonePaymentResponseDto portOnePayment = portonePaymentClient.payWithBillingKey(
                billingKey, paymentId, orderName, price, user.getId());

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneMonthLater = now.plusMinutes(10); //10분으로 테스트만 할 예정

        // 결제 승인은 이미 끝났으므로 여기서부터는 반드시 커밋되어야 한다.
        Subscription subscription = new TransactionTemplate(transactionManager).execute(status ->
                persistActivatedSubscription(user, request, price, paymentId, portOnePayment, now, oneMonthLater));

        // 다음(2회차) 결제를 PortOne 예약결제 API로 미리 예약해둔다.
        // 위 트랜잭션이 이미 커밋된 뒤라, 여기서 실패해도 방금 확정된 결제/구독 기록은 그대로 남는다.
        try {
            createNextSchedule(subscription, billingKey, billingKeyEntity.getId(), orderName, price, 2, oneMonthLater);
        } catch (Exception e) {
            log.error("[구독 1회차 결제 성공 - 다음 회차 예약 실패] subscriptionId: {}, userId: {}",
                    subscription.getId(), userId, e);
            // SubscriptionExpirationScheduler의 recoverMissingSchedules()가 주기적으로 재시도한다.
        }

        return SubscriptionResponseDto.from(subscription);
    }

    private Subscription persistActivatedSubscription(
            User user, SubscriptionCreateRequestDto request, int price, String paymentId,
            PortonePaymentResponseDto portOnePayment, LocalDateTime now, LocalDateTime oneMonthLater
    ) {
        Subscription subscription = Subscription.builder()
                .user(user)
                .planType(request.getPlanType())
                .status(SubscriptionStatus.ACTIVE)
                .currentPeriodStart(now)
                .currentPeriodEnd(oneMonthLater)
                .cancelAtPeriodEnd(false)
                .build();

        subscriptionRepository.save(subscription);

        // 구독에 매핑되는 수강권(SubscriptionPass) 엔티티 발급
        int passCount = getPassCount(request.getPlanType());

        SubscriptionPass pass = SubscriptionPass.builder()
                .subscription(subscription)
                .totalCount(passCount)
                .remainingCount(passCount)
                .validFrom(now)
                .validUntil(oneMonthLater)
                .status(PassStatus.ACTIVE)
                .build();

        subscriptionPassRepository.save(pass);

        // 1회차 결제도 결제 내역/정산 조회에서 동일하게 보이도록 Order/Payment 기록을 남긴다
        // (2회차 이후는 renewSubscription에서 동일한 패턴으로 기록됨).
        Order order = Order.builder()
                .user(user)
                .amount(price)
                .merchantOrderId(paymentId)
                .orderType(OrderType.SUBSCRIPTION)
                .orderStatus(OrderStatus.PAID)
                .subscription(subscription)
                .build();
        orderRepository.save(order);

        Payment payment = Payment.builder()
                .order(order)
                .portonePaymentId(paymentId)
                .payMethod(portOnePayment.getPayMethodType())
                .amount(price)
                .status(PaymentStatus.PAID)
                .approvedAt(portOnePayment.getApprovedAtParsed() != null ? portOnePayment.getApprovedAtParsed() : now)
                .build();
        paymentRepository.save(payment);

        return subscription;
    }

    /**
     * 다음 회차 결제를 PortOne 예약결제 API로 예약하고 SubscriptionSchedule을 저장한다.
     * 회차마다 새 paymentId를 발급하며, 이 paymentId는 결제 실행 후 도착하는 웹훅에서
     * 해당 회차를 역추적하는 데 사용된다({@link #renewSubscription}).
     */
    private void createNextSchedule(
            Subscription subscription, String billingKey, Long billingKeyId,
            String orderName, int amount, int round, LocalDateTime scheduledAt
    ) {
        String nextPaymentId = "BE24-CITYFARM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        PortonePaymentScheduleResponseDto scheduleResponse = portonePaymentClient.schedulePayment(
                billingKey, nextPaymentId, orderName, amount, subscription.getUser().getId(), scheduledAt
        );

        SubscriptionSchedule schedule = SubscriptionSchedule.builder()
                .subscription(subscription)
                .round(round)
                .scheduledAt(scheduledAt)
                .amount(amount)
                .status(ScheduleStatus.SCHEDULED)
                .paymentId(nextPaymentId)
                .billingKeyId(billingKeyId)
                .portoneScheduleId(scheduleResponse != null && scheduleResponse.getSchedule() != null
                        ? scheduleResponse.getSchedule().getId() : null)
                .build();

        subscriptionScheduleRepository.save(schedule);
    }

    /**
     * PortOne 예약결제가 실행되어 Transaction.Paid 웹훅이 도착했을 때 호출된다.
     * paymentId로 SubscriptionSchedule을 찾아 구독 기간/수강권을 연장하고, 해지 예약이 없다면
     * 다음 회차를 다시 예약한다. 매핑되는 스케줄이 없으면(=구독 결제가 아니면) false를 반환한다.
     *
     * 이 회차의 결제는 PortOne이 이미 실제로 실행한 뒤 도착한 웹훅이라(createSubscription과
     * 동일한 이유로), 회차 반영(applyRenewal)까지는 별도 트랜잭션으로 커밋을 확정하고 다음
     * 회차 예약은 그 밖에서 시도한다 — 예약이 실패해도 이미 실행된 이번 회차의 갱신 기록이
     * 롤백되지 않게 하기 위함.
     */
    public boolean renewSubscription(String paymentId, PortonePaymentResponseDto portOnePayment) {
        RenewalOutcome outcome = new TransactionTemplate(transactionManager)
                .execute(status -> applyRenewal(paymentId, portOnePayment));

        if (outcome == null) {
            return false;
        }
        if (!outcome.scheduleNext()) {
            return true;
        }

        try {
            createNextSchedule(outcome.subscription(), outcome.billingKey(), outcome.billingKeyId(),
                    outcome.orderName(), outcome.price(), outcome.round(), outcome.newPeriodEnd());
        } catch (Exception e) {
            log.error("[구독 {}회차 결제 성공 - 다음 회차 예약 실패] subscriptionId: {}",
                    outcome.round(), outcome.subscription().getId(), e);
            // SubscriptionExpirationScheduler의 recoverMissingSchedules()가 주기적으로 재시도한다.
        }

        return true;
    }

    private RenewalOutcome applyRenewal(String paymentId, PortonePaymentResponseDto portOnePayment) {
        SubscriptionSchedule schedule = subscriptionScheduleRepository.findByPaymentId(paymentId).orElse(null);
        if (schedule == null) {
            return null;
        }

        // 멱등성: 이미 처리된 회차라면 스킵 (동일 웹훅 재전송 등 방어)
        if (schedule.getStatus() == ScheduleStatus.PAID) {
            log.info("[구독 갱신] 이미 처리된 회차입니다. paymentId: {}", paymentId);
            return RenewalOutcome.noFurtherAction();
        }

        Integer actualPaidAmount = portOnePayment.getAmount() != null ? portOnePayment.getAmount().getPaid() : null;
        if (actualPaidAmount == null || !actualPaidAmount.equals(schedule.getAmount())) {
            log.error("[구독 갱신 결제 위변조 위험] 결제 금액 불일치. Schedule: {}, PortOne: {}", schedule.getAmount(), actualPaidAmount);
            return RenewalOutcome.noFurtherAction();
        }

        Subscription subscription = schedule.getSubscription();
        User user = subscription.getUser();

        // 구독 결제용 Order/Payment 기록 (원데이 클래스 주문과 동일한 테이블을 공유)
        Order order = Order.builder()
                .user(user)
                .amount(schedule.getAmount())
                .merchantOrderId(paymentId)
                .orderType(OrderType.SUBSCRIPTION)
                .orderStatus(OrderStatus.PAID)
                .subscription(subscription)
                .build();
        orderRepository.save(order);

        Payment payment = Payment.builder()
                .order(order)
                .portonePaymentId(paymentId)
                .payMethod(portOnePayment.getPayMethodType())
                .amount(schedule.getAmount())
                .status(PaymentStatus.PAID)
                .approvedAt(portOnePayment.getApprovedAtParsed() != null ? portOnePayment.getApprovedAtParsed() : LocalDateTime.now())
                .build();
        paymentRepository.save(payment);

        schedule.setStatus(ScheduleStatus.PAID);
        schedule.setOrder(order);

        // 구독 기간 연장
        LocalDateTime newPeriodStart = schedule.getScheduledAt();
        LocalDateTime newPeriodEnd = newPeriodStart.plusMonths(1);
        subscription.setCurrentPeriodStart(newPeriodStart);
        subscription.setCurrentPeriodEnd(newPeriodEnd);

        // 이전 회차 수강권 만료 처리 후 새 회차 수강권 발급
        subscriptionPassRepository.findBySubscriptionIdAndStatus(subscription.getId(), PassStatus.ACTIVE)
                .ifPresent(oldPass -> oldPass.setStatus(PassStatus.EXPIRED));

        int passCount = getPassCount(subscription.getPlanType());
        SubscriptionPass newPass = SubscriptionPass.builder()
                .subscription(subscription)
                .totalCount(passCount)
                .remainingCount(passCount)
                .validFrom(newPeriodStart)
                .validUntil(newPeriodEnd)
                .status(PassStatus.ACTIVE)
                .build();
        subscriptionPassRepository.save(newPass);

        log.info("[구독 갱신 완료] subscriptionId: {}, round: {}, paymentId: {}", subscription.getId(), schedule.getRound(), paymentId);

        if (subscription.isCancelAtPeriodEnd()) {
            // 해지 예약된 구독은 이번 회차를 마지막으로 종료하고 다음 회차를 예약하지 않는다.
            subscription.setStatus(SubscriptionStatus.CANCELLED);
            subscription.setCancelledAt(LocalDateTime.now());
            return RenewalOutcome.noFurtherAction();
        }

        BillingKey billingKeyEntity = billingKeyRepository.findByUserIdAndStatus(user.getId(), BillingKeyStatus.ACTIVE).orElse(null);
        if (billingKeyEntity == null) {
            log.error("[구독 갱신] 다음 회차 예약 실패 - 빌링키를 찾을 수 없음. userId: {}", user.getId());
            return RenewalOutcome.noFurtherAction();
        }

        String orderName = subscription.getPlanType().name() + " 정기구독";
        int price = calculatePrice(subscription.getPlanType());
        return new RenewalOutcome(true, subscription, billingKeyEntity.getBillingKeyEncrypted(), billingKeyEntity.getId(),
                orderName, price, schedule.getRound() + 1, newPeriodEnd);
    }

    /**
     * applyRenewal()의 트랜잭션이 커밋된 뒤, 다음 회차를 예약해야 하는지와 그때 필요한 값을
     * 트랜잭션 밖으로 들고 나오기 위한 결과 객체. scheduleNext=false면 subscription 이하
     * 필드는 채워지지 않는다.
     */
    private record RenewalOutcome(
            boolean scheduleNext,
            Subscription subscription,
            String billingKey,
            Long billingKeyId,
            String orderName,
            int price,
            int round,
            LocalDateTime newPeriodEnd
    ) {
        static RenewalOutcome noFurtherAction() {
            return new RenewalOutcome(false, null, null, null, null, 0, 0, null);
        }
    }

    /**
     * PortOne 예약결제가 실패하여 Transaction.Failed 웹훅이 도착했을 때 호출된다.
     * 매핑되는 스케줄이 없으면(=구독 결제가 아니면) false를 반환한다.
     */
    @Transactional
    public boolean handleScheduleFailed(String paymentId) {
        SubscriptionSchedule schedule = subscriptionScheduleRepository.findByPaymentId(paymentId).orElse(null);
        if (schedule == null) {
            return false;
        }

        if (schedule.getStatus() == ScheduleStatus.FAILED || schedule.getStatus() == ScheduleStatus.PAID) {
            return true;
        }

        schedule.setStatus(ScheduleStatus.FAILED);
        log.error("[구독 갱신 결제 실패] subscriptionId: {}, round: {}, paymentId: {}",
                schedule.getSubscription().getId(), schedule.getRound(), paymentId);

        return true;
    }

    /**
     * 3. 정기 구독 해지/환불 요청 (사용자가 누르는 "구독 해지" 버튼의 실제 진입점)
     * 정책: 결제 승인 시각 기준 24시간 이내이고, 이번 회차 수강권을 한 번도 쓰지 않았다면
     * 전액 환불 + 즉시 해지. 그 외에는 기존과 동일하게 다음 결제일에 해지 예약되고,
     * 남은 수강권은 해지일까지 그대로 유지된다. 이 판정은 매 갱신 회차마다 동일하게 적용된다
     * (최초 가입 때만이 아니라, 매달 새로 발급되는 회차 결제/수강권 기준으로 24시간이 다시 열림).
     */
    @Transactional
    public SubscriptionCancelResponseDto requestCancellation(Long userId, Long subscriptionId) {
        // 환불 자격 판정(미사용 여부) 도중 다른 트랜잭션이 이 구독/수강권을 건드리지 못하도록 락
        Subscription subscription = subscriptionRepository.findByIdForUpdate(subscriptionId)
                .orElseThrow(() -> new CustomException(CustomError.SUBSCRIPTION_NOT_FOUND));

        if (!subscription.getUser().getId().equals(userId)) {
            throw new CustomException(CustomError.AUTH_UNAUTHORIZED);
        }

        if (subscription.getStatus() == SubscriptionStatus.ACTIVE && !subscription.isCancelAtPeriodEnd()) {
            SubscriptionPass pass = subscriptionPassRepository
                    .findBySubscriptionIdAndStatusForUpdate(subscription.getId(), PassStatus.ACTIVE)
                    .orElse(null);

            if (pass != null && isRefundEligible(subscription, pass)) {
                int refundedAmount = refundCurrentPeriod(subscription, pass);
                return SubscriptionCancelResponseDto.of(SubscriptionCancelResultType.REFUNDED, subscription, refundedAmount);
            }
        }

        // 환불 대상이 아니면 기존 "해지 예약" 흐름 그대로 적용
        cancelSubscriptionAtPeriodEnd(userId, subscriptionId);
        return SubscriptionCancelResponseDto.of(SubscriptionCancelResultType.SCHEDULED_CANCEL, subscription, null);
    }

    /**
     * 이번 회차 결제 승인 후 24시간 이내이고, 이번 회차에 발급된 수강권을 한 번도 쓰지 않았는지 확인.
     */
    private boolean isRefundEligible(Subscription subscription, SubscriptionPass pass) {
        if (subscriptionPassUsageRepository.existsBySubscriptionPassId(pass.getId())) {
            return false;
        }

        Payment payment = orderRepository.findTopBySubscriptionIdOrderByCreatedAtDesc(subscription.getId())
                .flatMap(order -> paymentRepository.findByOrderId(order.getId()))
                .orElse(null);

        if (payment == null || payment.getStatus() != PaymentStatus.PAID || payment.getApprovedAt() == null) {
            return false;
        }

        return payment.getApprovedAt().plusHours(REFUND_ELIGIBLE_HOURS).isAfter(LocalDateTime.now());
    }

    /**
     * 이번 회차 결제를 PortOne에서 전액 취소하고, 구독을 즉시 종료 처리한다.
     * isRefundEligible로 자격 확인이 끝난 뒤에만 호출되어야 한다.
     */
    private int refundCurrentPeriod(Subscription subscription, SubscriptionPass pass) {
        Order currentOrder = orderRepository.findTopBySubscriptionIdOrderByCreatedAtDesc(subscription.getId())
                .orElseThrow(() -> new CustomException(CustomError.PAYMENT_NOT_FOUND));
        Payment payment = paymentRepository.findByOrderId(currentOrder.getId())
                .orElseThrow(() -> new CustomException(CustomError.PAYMENT_NOT_FOUND));

        String reason = "구독 24시간 이내 미사용 환불";
        portonePaymentClient.cancelPayment(payment.getPortonePaymentId(), reason);

        LocalDateTime now = LocalDateTime.now();
        payment.cancel(reason, now);
        currentOrder.setOrderStatus(OrderStatus.CANCELLED);

        // 다음 회차 예약결제도 더는 필요 없으므로 함께 취소
        cancelScheduledNextPayment(subscription.getId());

        subscription.setStatus(SubscriptionStatus.CANCELLED);
        subscription.setCancelledAt(now);
        pass.setStatus(PassStatus.EXPIRED);

        log.info("[구독 24시간 환불 완료] subscriptionId: {}, orderId: {}, amount: {}",
                subscription.getId(), currentOrder.getId(), payment.getAmount());

        return payment.getAmount();
    }

    /**
     * 정기 구독 해지 예약 (다음 결제일에 갱신 방지)
     * 다음 회차 결제는 가입/직전 갱신 시점에 이미 PortOne에 예약되어 있으므로,
     * DB 플래그만 세우는 것으로는 다음 결제를 막을 수 없다. 예약된 PortOne 스케줄을
     * 함께 취소해야 실제로 "다음 결제일에 갱신 방지"가 된다.
     */
    @Transactional
    public void cancelSubscriptionAtPeriodEnd(Long userId, Long subscriptionId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new CustomException(CustomError.SUBSCRIPTION_NOT_FOUND));

        // 본인 구독권인지 검증
        if (!subscription.getUser().getId().equals(userId)) {
            throw new CustomException(CustomError.AUTH_UNAUTHORIZED);
        }

        if (subscription.isCancelAtPeriodEnd()) {
            return; // 이미 해지 예약된 구독 (멱등 처리)
        }

        cancelScheduledNextPayment(subscriptionId);

        // 다음 주기에 해지되도록 상태 변경
        subscription.setCancelAtPeriodEnd(true);
    }

    private void cancelScheduledNextPayment(Long subscriptionId) {
        subscriptionScheduleRepository.findBySubscriptionIdAndStatus(subscriptionId, ScheduleStatus.SCHEDULED)
                .ifPresent(schedule -> {
                    if (schedule.getPortoneScheduleId() != null) {
                        portonePaymentClient.cancelSchedule(schedule.getPortoneScheduleId());
                    }
                    schedule.setStatus(ScheduleStatus.CANCELLED);
                });
    }

    /**
     * [카드 변경] 활성 구독의 SCHEDULED 예약을 새 빌링키로 옮긴다.
     * 예약이 없으면(구독이 없거나 이미 실행/취소된 상태) 아무 것도 하지 않는다.
     * PortOne이 예약 걸린 빌링키의 삭제를 막기 때문에(409 PaymentScheduleAlreadyExistsError),
     * BillingKeyService가 이전 키를 지우기 전에 반드시 먼저 호출해야 한다.
     * 실패하면 예외를 그대로 던져서(트랜잭션 롤백) 호출부가 새 카드 저장까지 함께 취소하도록 한다.
     */
    @Transactional
    public void migrateActiveScheduleToNewBillingKey(Long userId, BillingKey newBillingKey) {
        Subscription subscription = subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE)
                .orElse(null);
        if (subscription == null) {
            return;
        }

        SubscriptionSchedule oldSchedule = subscriptionScheduleRepository
                .findBySubscriptionIdAndStatus(subscription.getId(), ScheduleStatus.SCHEDULED)
                .orElse(null);
        if (oldSchedule == null || oldSchedule.getStatus() != ScheduleStatus.SCHEDULED) {
            return;
        }

        if (oldSchedule.getPortoneScheduleId() != null) {
            portonePaymentClient.cancelSchedule(oldSchedule.getPortoneScheduleId());
        }
        oldSchedule.setStatus(ScheduleStatus.CANCELLED);

        String orderName = subscription.getPlanType().name() + " 정기구독";
        createNextSchedule(
                subscription, newBillingKey.getBillingKeyEncrypted(), newBillingKey.getId(),
                orderName, oldSchedule.getAmount(), oldSchedule.getRound(), oldSchedule.getScheduledAt()
        );

        log.info("[카드 변경 - 예약 마이그레이션 완료] subscriptionId: {}, round: {}", subscription.getId(), oldSchedule.getRound());
    }

    /**
     * [빌링키 단독 삭제 가드용] 이 유저의 활성 구독에 아직 실행되지 않은(SCHEDULED) 예약결제가
     * 걸려있는지 확인한다. 걸려있으면 대체 카드 없이 빌링키를 삭제해선 안 된다(다음 회차 결제
     * 수단이 없어짐). BillingKeyService.revokeMyBillingKey에서 사용.
     */
    @Transactional(readOnly = true)
    public boolean hasActiveScheduledPayment(Long userId) {
        return subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE)
                .map(subscription -> subscriptionScheduleRepository
                        .findBySubscriptionIdAndStatus(subscription.getId(), ScheduleStatus.SCHEDULED)
                        .isPresent())
                .orElse(false);
    }

    /**
     * 4. 특정 구독권의 패스(이용권) 단건 조회
     */
    @Transactional(readOnly = true)
    public PassResponseDto getSubscriptionPass(Long userId, Long subscriptionId) {
        SubscriptionPass pass = subscriptionPassRepository.findBySubscriptionIdAndStatus(subscriptionId, PassStatus.ACTIVE)
                .orElseThrow(() -> new CustomException(CustomError.SUBSCRIPTION_PASS_NOT_FOUND));

        // 본인의 수강권인지 검증
        if (!pass.getSubscription().getUser().getId().equals(userId)) {
            throw new CustomException(CustomError.AUTH_UNAUTHORIZED);
        }

        return PassResponseDto.from(pass);
    }

    /**
     * 5. 구독 수강권으로 원데이클래스 신청 (결제 없이 즉시 확정)
     * 클래스 가격과 무관하게 보유 수강권 1개를 차감하고 수강 신청을 CONFIRMED로 생성한다.
     */
    @Transactional
    public MyEnrollmentResponseDto enrollClassWithPass(Long userId, Long classId) {
        Subscription subscription = subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE)
                .orElseThrow(() -> new CustomException(CustomError.SUBSCRIPTION_NOT_FOUND));

        SubscriptionPass pass = subscriptionPassRepository
                .findBySubscriptionIdAndStatusAndRemainingCountGreaterThan(subscription.getId(), PassStatus.ACTIVE, 0)
                .orElseThrow(() -> new CustomException(CustomError.SUBSCRIPTION_PASS_EXHAUSTED));

        // 정원/중복 검증 및 CONFIRMED 상태 수강 신청 생성 (락은 ClassEnrollmentService가 담당)
        ClassEnrollment enrollment = classEnrollmentService.createConfirmedEnrollmentByPass(userId, classId, subscription.getId());

        // 수강권 차감
        pass.setRemainingCount(pass.getRemainingCount() - 1);
        if (pass.getRemainingCount() <= 0) {
            pass.setStatus(PassStatus.EXHAUSTED);
        }

        SubscriptionPassUsage usage = SubscriptionPassUsage.builder()
                .subscriptionPass(pass)
                .enrollment(enrollment)
                .usedAt(LocalDateTime.now())
                .build();
        subscriptionPassUsageRepository.save(usage);

        // 수강권으로 들은 클래스도 일반결제와 동일하게 호스트 정산 대상에 포함한다(정책 확정 사항).
        // 결제(Order/Payment)가 없는 경로라 클래스 가격을 정산 기준 금액으로 사용한다.
        OneDayClass oneDayClass = enrollment.getOneDayClass();
        settlementService.createPendingSettlementForPass(oneDayClass.getHost(), classId, oneDayClass.getPrice(), enrollment.getId());

        log.info("[구독 수강권 사용] userId: {}, subscriptionId: {}, passId: {}, classId: {}, 잔여: {}",
                userId, subscription.getId(), pass.getId(), classId, pass.getRemainingCount());

        return MyEnrollmentResponseDto.from(enrollment);
    }

    /**
     * 6. 특정 수강권의 사용 내역 조회
     */
    @Transactional(readOnly = true)
    public List<SubscriptionPassUsageResponseDto> getPassUsages(Long userId, Long passId) {
        SubscriptionPass pass = subscriptionPassRepository.findById(passId)
                .orElseThrow(() -> new CustomException(CustomError.SUBSCRIPTION_PASS_NOT_FOUND));

        if (!pass.getSubscription().getUser().getId().equals(userId)) {
            throw new CustomException(CustomError.AUTH_UNAUTHORIZED);
        }

        return subscriptionPassUsageRepository.findBySubscriptionPassIdOrderByCreatedAtDesc(passId).stream()
                .map(SubscriptionPassUsageResponseDto::from)
                .toList();
    }

    /**
     * 7. [수강권 복구] enrollmentId 기준
     * ClassEnrollment 쪽에서 수강권으로 신청한 건을 취소할 때 호출한다. 사용했던 수강권의
     * remainingCount를 1 되돌리고, EXHAUSTED 상태였다면 ACTIVE로 되돌린다(EXPIRED는 그대로 둠 —
     * 이미 기간이 지난 수강권은 복구돼도 다시 쓸 수 없어야 함). 소유권 검증은 호출부
     * (ClassEnrollmentService)에서 이미 끝났다고 가정하고 여기서는 하지 않는다.
     */
    @Transactional
    public void restorePassByEnrollmentId(Long enrollmentId) {
        SubscriptionPassUsage usage = subscriptionPassUsageRepository.findByEnrollmentId(enrollmentId)
                .orElseThrow(() -> new CustomException(CustomError.SUBSCRIPTION_PASS_NOT_FOUND));

        SubscriptionPass pass = usage.getSubscriptionPass();
        pass.setRemainingCount(pass.getRemainingCount() + 1);
        if (pass.getStatus() == PassStatus.EXHAUSTED) {
            pass.setStatus(PassStatus.ACTIVE);
        }

        subscriptionPassUsageRepository.delete(usage);

        // GENERAL 결제 취소(PaymentService.refundForHostCancelledClass)와 대칭되게, 이 신청 건으로
        // 발생한 Settlement도 함께 취소한다 — 안 그러면 호스트 클래스 취소 시 PENDING에 계속 남는다.
        settlementService.cancelSettlementByEnrollmentId(enrollmentId);

        log.info("[구독 수강권 복구] enrollmentId: {}, passId: {}, 복구 후 잔여: {}",
                enrollmentId, pass.getId(), pass.getRemainingCount());
    }

    /**
     * 8. [구독 만료 배치] 해지 예약(cancelAtPeriodEnd=true)됐고 currentPeriodEnd가 지난 ACTIVE
     * 구독을 CANCELLED로 정리한다. 예약결제 자체는 해지 예약 시점에 이미 취소돼서 더 이상
     * 웹훅이 오지 않으므로(=renewSubscription이 호출될 일이 없으므로), 이 상태 전환은
     * PortOne 웹훅이 아니라 이 배치가 전담한다. SubscriptionExpirationScheduler에서 주기적으로 호출.
     */
    @Transactional
    public int expireCancelledSubscriptions() {
        List<Subscription> targets = subscriptionRepository
                .findByStatusAndCancelAtPeriodEndTrueAndCurrentPeriodEndBefore(
                        SubscriptionStatus.ACTIVE, LocalDateTime.now());

        LocalDateTime now = LocalDateTime.now();
        for (Subscription subscription : targets) {
            subscription.setStatus(SubscriptionStatus.CANCELLED);
            subscription.setCancelledAt(now);
        }

        if (!targets.isEmpty()) {
            log.info("[구독 만료 배치] {}건 CANCELLED 처리", targets.size());
        }

        return targets.size();
    }

    /**
     * 9. [구독 예약 복구 배치] createSubscription()에서 1회차 결제는 성공했는데 그 직후
     * PortOne 예약(다음 회차)이 실패해서 SCHEDULED 상태 SubscriptionSchedule이 하나도
     * 없는 채로 남은 ACTIVE 구독을 찾아 재예약을 시도한다. SubscriptionExpirationScheduler에서
     * 주기적으로 호출.
     */
    @Transactional
    public int recoverMissingSchedules() {
        List<Subscription> targets = subscriptionRepository.findActiveWithoutScheduledPayment(SubscriptionStatus.ACTIVE);

        for (Subscription subscription : targets) {
            BillingKey billingKeyEntity = billingKeyRepository
                    .findByUserIdAndStatus(subscription.getUser().getId(), BillingKeyStatus.ACTIVE)
                    .orElse(null);

            if (billingKeyEntity == null) {
                log.error("[구독 예약 복구 실패] 빌링키를 찾을 수 없음. subscriptionId: {}", subscription.getId());
                continue;
            }

            String orderName = subscription.getPlanType().name() + " 정기구독";
            int price = calculatePrice(subscription.getPlanType());

            try {
                // 최초 가입 직후에만 발생하는 상황이라 회차는 항상 2회차다 (createSubscription 참고).
                createNextSchedule(subscription, billingKeyEntity.getBillingKeyEncrypted(), billingKeyEntity.getId(),
                        orderName, price, 2, subscription.getCurrentPeriodEnd());
                log.info("[구독 예약 복구 성공] subscriptionId: {}", subscription.getId());
            } catch (Exception e) {
                log.error("[구독 예약 복구 실패] subscriptionId: {}", subscription.getId(), e);
            }
        }

        if (!targets.isEmpty()) {
            log.info("[구독 예약 복구 배치] {}건 시도", targets.size());
        }

        return targets.size();
    }

    // ==========================================
    // 헬퍼 메서드 (비즈니스 요구사항에 맞게 수정)
    // ==========================================

    /**
     * PlanType에 따른 결제 금액 반환
     */
    private int calculatePrice(SubscriptionPlanType planType) {
        if (planType == null) return 0;

        switch (planType) {
            // 예시: (프로젝트 설정에 맞춰 주석 해제 및 수정해주세요)
             case BASIC: return 30000;
            // case PREMIUM: return 20000;
            default: return 30000; // 테스트용 금액 100원
        }
    }

    /**
     * PlanType에 따른 지급 수강권 횟수 반환
     */
    private int getPassCount(SubscriptionPlanType planType) {
        if (planType == null) return 0;

        switch (planType) {
             case BASIC: return 3;
            // case PREMIUM: return 3;
            default: return 3;
        }
    }
}