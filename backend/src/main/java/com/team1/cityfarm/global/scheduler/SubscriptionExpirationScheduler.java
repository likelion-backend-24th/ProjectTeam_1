package com.team1.cityfarm.global.scheduler;

import com.team1.cityfarm.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * PortOne 예약결제와는 별개로, 우리 DB의 Subscription.status가 오래돼서(stale) 안 맞는 걸
 * 주기적으로 청소하는 내부 배치. 해지 예약된 구독은 다음 회차 PortOne 예약을 이미 취소해뒀기
 * 때문에 더 이상 웹훅이 오지 않으며, 그 상태 전환을 대신 이 스케줄러가 담당한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionExpirationScheduler {

    private final SubscriptionService subscriptionService;

    @Scheduled(cron = "0 * * * * *") // 매분 정각
    public void expireCancelledSubscriptions() {
        subscriptionService.expireCancelledSubscriptions();
    }

    // 1회차 결제 성공 뒤 다음 회차 예약이 실패해 방치된 구독을 재예약 시도한다.
    @Scheduled(cron = "0 * * * * *") // 매분 정각
    public void recoverMissingSchedules() {
        subscriptionService.recoverMissingSchedules();
    }
}
