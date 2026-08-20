package com.team1.cityfarm.controller;

import com.team1.cityfarm.dto.PaymentCancelRequestDto;
import com.team1.cityfarm.dto.PaymentResponseDto;
import com.team1.cityfarm.dto.PaymentVerifyRequestDto;
import com.team1.cityfarm.global.security.user.CustomUserDetails;
import com.team1.cityfarm.portone.RefundEligibilityResponseDto;
import com.team1.cityfarm.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    //결제 검증 및 완료
    @PostMapping("/verify")
    public ResponseEntity<PaymentResponseDto> verifyAndCompletePayment(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestBody PaymentVerifyRequestDto request
    ) {
        PaymentResponseDto response = paymentService.verifyAndCompletePayment(request);
        return ResponseEntity.ok(response);
    }

    //환불 가능 여부 및 사유 조회
    @GetMapping("/{paymentId}/refund-eligibility")
    public ResponseEntity<RefundEligibilityResponseDto> checkRefundEligibility(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @PathVariable Long paymentId
    ) {
        RefundEligibilityResponseDto response = paymentService.checkRefundEligibility(paymentId);
        return ResponseEntity.ok(response);
    }

    //결제 전체 취소 및 환불
    @PostMapping("/{paymentId}/cancel")
    public ResponseEntity<PaymentResponseDto> cancelPayment(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @PathVariable Long paymentId,
            @RequestBody(required = false) PaymentCancelRequestDto request
    ) {
        PaymentResponseDto response = paymentService.cancelPayment(customUserDetails.getUserId(), paymentId, request);
        return ResponseEntity.ok(response);
    }
}