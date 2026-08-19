package com.team1.cityfarm.entity;

public enum SettlementStatus {
    PENDING,    // 정산 대기 (결제 완료 직후)
    COMPLETED,  // 정산 지급 완료
    CANCELLED   // 취소됨 (결제 환불 시)
}