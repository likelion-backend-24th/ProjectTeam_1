package com.team1.cityfarm.dto;

import com.team1.cityfarm.entity.Settlement;
import com.team1.cityfarm.entity.SettlementStatus;
import com.team1.cityfarm.entity.SettlementType;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
public class SettlementResponseDto {

    private final Long id;
    private final Long orderId;
    private final SettlementType settlementType;
    private final int paymentAmount;       // 유저가 결제한 금액
    private final BigDecimal settlementRate; // 정산 비율 (예: 50%)
    private final int settlementAmount;    // 호스트에게 정산될 금액
    private final SettlementStatus status; // 정산 상태 (PENDING, COMPLETED, CANCELLED)
    private final LocalDateTime settledAt; // 정산 완료 시각
    private final LocalDateTime createdAt; // 정산 생성 시각

    public SettlementResponseDto(Settlement settlement) {
        this.id = settlement.getId();
        this.orderId = settlement.getOrder() != null ? settlement.getOrder().getId() : null;
        this.settlementType = settlement.getSettlementType();
        this.paymentAmount = settlement.getPaymentAmount();
        this.settlementRate = settlement.getSettlementRate();
        this.settlementAmount = settlement.getSettlementAmount();
        this.status = settlement.getStatus();
        this.settledAt = settlement.getSettledAt();
        this.createdAt = settlement.getCreatedAt();
    }
}