package com.team1.cityfarm.global.scheduler;

import com.team1.cityfarm.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 결제창을 열지 않고 이탈하거나 결제 도중 이탈해서 PENDING 상태로 남은 원데이클래스 주문을
 * 주기적으로 정리하는 내부 배치. PENDING 신청은 정원 점유로 카운트되므로, 방치된 채로 두면
 * 실제로는 결제하지 않은 사용자 때문에 다른 사용자가 정원 초과로 신청하지 못하게 된다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderExpirationScheduler {

    private final OrderService orderService;

    @Scheduled(cron = "0 * * * * *") // 매분 정각
    public void expireStalePendingOrders() {
        int expiredCount = orderService.expireStalePendingOrders();
        if (expiredCount > 0) {
            log.info("[주문 만료 배치] 결제 대기 시간 초과로 {}건의 주문을 만료 처리했습니다.", expiredCount);
        }
    }
}
