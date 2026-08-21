package com.team1.cityfarm.service;

import com.team1.cityfarm.dto.SettlementResponseDto;
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
import java.util.List;
import java.util.stream.Collectors;

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

    /**
     * [호스트용] 내 정산 내역 목록 조회
     */
    public List<SettlementResponseDto> getHostSettlements(Long hostId) {
        List<Settlement> settlements = settlementRepository.findByHostIdOrderByCreatedAtDesc(hostId);
        return settlements.stream()
                .map(SettlementResponseDto::new) // 혹은 프로젝트 내 정산 응답 DTO 변환 방식에 맞게 조정
                .collect(Collectors.toList());
    }

    /**
     * [관리자용] 정산 내역 조회
     */
    public List<SettlementResponseDto> getAdminSettlements() {
        List<Settlement> settlements = settlementRepository.findAllByOrderByIdDesc();

        return settlements.stream()
                .map(SettlementResponseDto::from) // 엔티티 -> DTO 변환 메서드 (프로젝트 스타일 맞춤)
                .toList();
    }

    /**
     * [관리자용] 정산 지급 완료 처리
     */
    @Transactional
    public void completeSettlement(Long settlementId) {
        Settlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new CustomException(CustomError.SETTLEMENT_NOT_FOUND));

        // 이미 완료되었거나 취소된 건인지 체크
        if (settlement.getStatus() == SettlementStatus.COMPLETED) {
            throw new CustomException(CustomError.ALREADY_COMPLETED_SETTLEMENT); // 혹은 적절한 에러 코드
        }

        // 엔티티에 구현되어 있는 complete() 메서드 호출 (상태 변경 + 완료 시각 갱신)
        settlement.complete();

        log.info("[정산 지급 완료 처리] Settlement ID: {}", settlement.getId());
    }
}