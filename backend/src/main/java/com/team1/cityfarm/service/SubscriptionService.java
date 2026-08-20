package com.team1.cityfarm.service;

import com.team1.cityfarm.dto.MyEnrollmentResponseDto;
import com.team1.cityfarm.dto.PassResponseDto;
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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionService {

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

    /**
     * 1. 내 활성 구독 정보 조회
     */
    @Transactional(readOnly = true)
    public SubscriptionResponseDto getMyActiveSubscription(Long userId) {
        Subscription subscription = subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE)
                .orElseThrow(() -> new IllegalArgumentException("활성화된 구독이 없습니다."));

        return SubscriptionResponseDto.from(subscription);
    }

    /**
     * 2. 정기 구독 신청 (빌링키 결제 및 수강권 발급)
     */
    @Transactional
    public SubscriptionResponseDto createSubscription(Long userId, SubscriptionCreateRequestDto request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));

        // 이미 활성화된 구독이 있는지 검증
        if (subscriptionRepository.existsByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE)) {
            throw new IllegalStateException("이미 활성화된 정기 구독이 존재합니다.");
        }

        // 요금제(PlanType)에 따른 결제 금액 설정
        int price = calculatePrice(request.getPlanType());
        String orderName = request.getPlanType().name() + " 정기구독";

        // 고유 결제 번호(paymentId) 생성
        String paymentId = "BE24-CITYFARM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // 포트원 빌링키 결제 요청 (실패 시 CustomException 발생 및 트랜잭션 롤백)
        PortonePaymentResponseDto portOnePayment = portonePaymentClient.payWithBillingKey(
                request.getBillingKey(), paymentId, orderName, price, user.getId());

        // 결제 성공 시 구독(Subscription) 엔티티 생성
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneMonthLater = now.plusMonths(1);

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

        // 다음(2회차) 결제를 PortOne 예약결제 API로 미리 예약해둔다.
        createNextSchedule(subscription, request.getBillingKey(), orderName, price, 2, oneMonthLater);

        return SubscriptionResponseDto.from(subscription);
    }

    /**
     * 다음 회차 결제를 PortOne 예약결제 API로 예약하고 SubscriptionSchedule을 저장한다.
     * 회차마다 새 paymentId를 발급하며, 이 paymentId는 결제 실행 후 도착하는 웹훅에서
     * 해당 회차를 역추적하는 데 사용된다({@link #renewSubscription}).
     */
    private void createNextSchedule(
            Subscription subscription, String billingKey, String orderName, int amount, int round, LocalDateTime scheduledAt
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
                .portoneScheduleId(scheduleResponse != null && scheduleResponse.getSchedule() != null
                        ? scheduleResponse.getSchedule().getId() : null)
                .build();

        subscriptionScheduleRepository.save(schedule);
    }

    /**
     * PortOne 예약결제가 실행되어 Transaction.Paid 웹훅이 도착했을 때 호출된다.
     * paymentId로 SubscriptionSchedule을 찾아 구독 기간/수강권을 연장하고, 해지 예약이 없다면
     * 다음 회차를 다시 예약한다. 매핑되는 스케줄이 없으면(=구독 결제가 아니면) false를 반환한다.
     */
    @Transactional
    public boolean renewSubscription(String paymentId, PortonePaymentResponseDto portOnePayment) {
        SubscriptionSchedule schedule = subscriptionScheduleRepository.findByPaymentId(paymentId).orElse(null);
        if (schedule == null) {
            return false;
        }

        // 멱등성: 이미 처리된 회차라면 스킵 (동일 웹훅 재전송 등 방어)
        if (schedule.getStatus() == ScheduleStatus.PAID) {
            log.info("[구독 갱신] 이미 처리된 회차입니다. paymentId: {}", paymentId);
            return true;
        }

        Integer actualPaidAmount = portOnePayment.getAmount() != null ? portOnePayment.getAmount().getPaid() : null;
        if (actualPaidAmount == null || !actualPaidAmount.equals(schedule.getAmount())) {
            log.error("[구독 갱신 결제 위변조 위험] 결제 금액 불일치. Schedule: {}, PortOne: {}", schedule.getAmount(), actualPaidAmount);
            return true;
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

        if (subscription.isCancelAtPeriodEnd()) {
            // 해지 예약된 구독은 이번 회차를 마지막으로 종료하고 다음 회차를 예약하지 않는다.
            subscription.setStatus(SubscriptionStatus.CANCELLED);
            subscription.setCancelledAt(LocalDateTime.now());
        } else {
            String billingKey = billingKeyRepository.findByUserId(user.getId())
                    .map(BillingKey::getBillingKeyEncrypted)
                    .orElse(null);

            if (billingKey == null) {
                log.error("[구독 갱신] 다음 회차 예약 실패 - 빌링키를 찾을 수 없음. userId: {}", user.getId());
            } else {
                String orderName = subscription.getPlanType().name() + " 정기구독";
                int price = calculatePrice(subscription.getPlanType());
                createNextSchedule(subscription, billingKey, orderName, price, schedule.getRound() + 1, newPeriodEnd);
            }
        }

        log.info("[구독 갱신 완료] subscriptionId: {}, round: {}, paymentId: {}", subscription.getId(), schedule.getRound(), paymentId);
        return true;
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
     * 3. 정기 구독 해지 예약 (다음 결제일에 갱신 방지)
     * 다음 회차 결제는 가입/직전 갱신 시점에 이미 PortOne에 예약되어 있으므로,
     * DB 플래그만 세우는 것으로는 다음 결제를 막을 수 없다. 예약된 PortOne 스케줄을
     * 함께 취소해야 실제로 "다음 결제일에 갱신 방지"가 된다.
     */
    @Transactional
    public void cancelSubscriptionAtPeriodEnd(Long userId, Long subscriptionId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("구독 정보를 찾을 수 없습니다."));

        // 본인 구독권인지 검증
        if (!subscription.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("권한이 없습니다.");
        }

        if (subscription.isCancelAtPeriodEnd()) {
            return; // 이미 해지 예약된 구독 (멱등 처리)
        }

        subscriptionScheduleRepository.findBySubscriptionIdAndStatus(subscriptionId, ScheduleStatus.SCHEDULED)
                .ifPresent(schedule -> {
                    if (schedule.getPortoneScheduleId() != null) {
                        portonePaymentClient.cancelSchedule(schedule.getPortoneScheduleId());
                    }
                    schedule.setStatus(ScheduleStatus.CANCELLED);
                });

        // 다음 주기에 해지되도록 상태 변경
        subscription.setCancelAtPeriodEnd(true);
    }

    /**
     * 4. 특정 구독권의 패스(이용권) 단건 조회
     */
    @Transactional(readOnly = true)
    public PassResponseDto getSubscriptionPass(Long userId, Long subscriptionId) {
        SubscriptionPass pass = subscriptionPassRepository.findBySubscriptionIdAndStatus(subscriptionId, PassStatus.ACTIVE)
                .orElseThrow(() -> new IllegalArgumentException("해당 구독의 수강권 정보를 찾을 수 없습니다."));

        // 본인의 수강권인지 검증
        if (!pass.getSubscription().getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("수강권 조회 권한이 없습니다.");
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