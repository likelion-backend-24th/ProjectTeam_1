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
     * PortOne SDK가 프론트엔드에 돌려준 발급 결과(billingKey)를 함께 전달받아
     * 백엔드가 PortOne API로 재조회·검증한 뒤에만 저장한다.
     */
    @PostMapping("/confirm")
    public ResponseEntity<BillingKeyResponseDto> confirmIssuance(
            @RequestParam("issueId") String issueId,
            @RequestParam("billingKey") String billingKey,
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        BillingKeyResponseDto response = billingKeyService.confirmIssuance(issueId, billingKey, customUserDetails.getUserId());
        return ResponseEntity.ok(response);
    }
}
