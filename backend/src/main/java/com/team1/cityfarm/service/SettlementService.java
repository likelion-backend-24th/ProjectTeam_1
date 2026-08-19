package com.team1.cityfarm.service;

import com.team1.cityfarm.entity.*;
import com.team1.cityfarm.global.exception.CustomError;
import com.team1.cityfarm.global.exception.CustomException;
import com.team1.cityfarm.repository.OneDayClassRepository;
import com.team1.cityfarm.repository.SettlementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SettlementService {

    private final SettlementRepository settlementRepository;
    private final OneDayClassRepository oneDayClassRepository;

    private static final BigDecimal CLASS_SETTLEMENT_RATE = new BigDecimal("50");

    /**
     * [결제 완료 시 호출] Order 기반 Host 대상 정산(PENDING) 데이터 생성
     */
    @Transactional
    public Settlement createPendingSettlement(Order order, Long classId) {

        // 1. 해당 주문건(orderId)에 대해 이미 정산 데이터가 존재하는지 검증
        if (settlementRepository.existsByOrderId(order.getId())) {
            throw new CustomException(CustomError.DUPLICATE_SETTLEMENT);
        }

        // 2. 클래스 조회를 통한 호스트(Host) 식별
        OneDayClass oneDayClass = oneDayClassRepository.findById(classId)
                .orElseThrow(() -> new CustomException(CustomError.ONE_DAY_CLASS_NOT_FOUND));

        User host = oneDayClass.getHost();

        // 3. 정산 금액 계산 (주문 결제 금액 * 50 / 100)
        BigDecimal paymentAmount = BigDecimal.valueOf(order.getAmount());

        BigDecimal settlementAmount = paymentAmount
                .multiply(CLASS_SETTLEMENT_RATE)
                .divide(new BigDecimal("100"), 0, RoundingMode.FLOOR); // 원 단위 이하 절사

        // 4. Settlement 엔티티 생성 및 저장 (실제 엔티티 Builder 필드 일치)
        Settlement settlement = Settlement.builder()
                .order(order)
                .host(host)
                .settlementType(SettlementType.ONE_DAY_CLASS)
                .paymentAmount(order.getAmount())
                .settlementRate(CLASS_SETTLEMENT_RATE)
                .settlementAmount(settlementAmount.intValue())
                .build();

        Settlement savedSettlement = settlementRepository.save(settlement);

        log.info("[정산 데이터 생성 완료] Settlement ID: {}, Host ID: {}, Order ID: {}, Settlement Amount: {}원",
                savedSettlement.getId(), host.getId(), order.getId(), settlementAmount.intValue());

        return savedSettlement;
    }
}