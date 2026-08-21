package com.team1.cityfarm.controller;

import com.team1.cityfarm.service.PortoneWebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/payments/webhooks/portone")
@RequiredArgsConstructor
public class PortoneWebhookController {

    private final PortoneWebhookService portoneWebhookService;

    /**
     * PortOne 웹훅 수신 엔드포인트
     * 헤더값(웹훅 ID, 서명)과 원본 JSON(rawPayload)을 그대로 전달합니다.
     */
    @PostMapping
    public ResponseEntity<Void> handleWebhook(
            @RequestHeader(value = "webhook-id", required = false) String webhookId,
            @RequestHeader(value = "webhook-timestamp", required = false) String timestamp,
            @RequestHeader(value = "webhook-signature", required = false) String signature,
            @RequestBody String rawPayload
    ) {
        log.info("[PortOne Webhook 수신] webhookId: {}", webhookId);

        portoneWebhookService.processWebhook(webhookId, timestamp, signature, rawPayload);

        // 웹훅은 항상 200 OK를 반환하여 포트원 측의 재시도를 방지해야 합니다.
        return ResponseEntity.ok().build();
    }
}