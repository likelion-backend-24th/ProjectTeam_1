package com.team1.cityfarm.controller;

import com.team1.cityfarm.dto.BillingKeyIntentResponseDto;
import com.team1.cityfarm.dto.BillingKeyResponseDto;
import com.team1.cityfarm.global.security.user.CustomUserDetails;
import com.team1.cityfarm.service.BillingKeyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/billing-keys")
@RequiredArgsConstructor
public class BillingKeyController {

    private final BillingKeyService billingKeyService;


    //내 빌링키 조회
    @GetMapping("/me")
    public ResponseEntity<BillingKeyResponseDto> getMyBillingKey(
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        BillingKeyResponseDto response = billingKeyService.getMyActiveBillingKey(customUserDetails.getUserId());
        return ResponseEntity.ok(response);
    }

    /**
     * [빌링키 발급 의도(Intent) 생성]
     * POST /api/billing-keys/intent
     */
    @PostMapping("/intent")
    public ResponseEntity<BillingKeyIntentResponseDto> createIssuanceIntent(
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        BillingKeyIntentResponseDto response = billingKeyService.createIssuanceIntent(customUserDetails.getUserId());
        return ResponseEntity.ok(response);
    }

    /**
     * [빌링키 발급 확정 및 저장]
     * POST /api/billing-keys/confirm
     */
    @PostMapping("/confirm")
    public ResponseEntity<BillingKeyResponseDto> confirmIssuance(
            @RequestParam("issueId") String issueId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        BillingKeyResponseDto response = billingKeyService.confirmIssuance(issueId, customUserDetails.getUserId());
        return ResponseEntity.ok(response);
    }
}
