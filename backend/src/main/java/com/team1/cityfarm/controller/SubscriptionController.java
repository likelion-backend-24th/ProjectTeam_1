package com.team1.cityfarm.controller;

import com.team1.cityfarm.dto.PassResponseDto;
import com.team1.cityfarm.dto.SubscriptionCreateRequestDto;
import com.team1.cityfarm.dto.SubscriptionResponseDto;
import com.team1.cityfarm.global.security.user.CustomUserDetails;
import com.team1.cityfarm.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    /**
     * 1. 내 활성 구독 정보 조회
     */
    @GetMapping("/me")
    public ResponseEntity<SubscriptionResponseDto> getMyActiveSubscription(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        SubscriptionResponseDto response = subscriptionService.getMyActiveSubscription(userDetails.getUserId());
        return ResponseEntity.ok(response);
    }

    /**
     * 2. 정기 구독 신청 (빌링키 결제 및 수강권 발급)
     */
    @PostMapping
    public ResponseEntity<SubscriptionResponseDto> createSubscription(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody SubscriptionCreateRequestDto request) {
        SubscriptionResponseDto response = subscriptionService.createSubscription(userDetails.getUserId(), request);
        return ResponseEntity.ok(response);
    }

    /**
     * 3. 정기 구독 해지 예약 (다음 주기 갱신 안 함)
     */
    @PostMapping("/{subscriptionId}/cancel")
    public ResponseEntity<Void> cancelAtPeriodEnd(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long subscriptionId) {
        subscriptionService.cancelSubscriptionAtPeriodEnd(userDetails.getUserId(), subscriptionId);
        return ResponseEntity.ok().build();
    }

    /**
     * 4. 특정 구독권의 패스(이용권) 단건 조회
     */
    @GetMapping("/{subscriptionId}/passes")
    public ResponseEntity<PassResponseDto> getSubscriptionPass(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long subscriptionId) {
        PassResponseDto pass = subscriptionService.getSubscriptionPass(userDetails.getUserId(), subscriptionId);
        return ResponseEntity.ok(pass);
    }
}