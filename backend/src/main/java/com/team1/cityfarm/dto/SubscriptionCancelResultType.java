package com.team1.cityfarm.dto;

public enum SubscriptionCancelResultType {
    // 24시간 이내 + 수강권 미사용 - 전액 환불, 구독 즉시 해지
    REFUNDED,
    // 그 외 - 환불 없이 다음 결제일에 해지 예약, 남은 수강권은 해지일까지 유지
    SCHEDULED_CANCEL
}
