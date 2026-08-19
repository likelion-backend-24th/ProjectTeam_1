package com.team1.cityfarm.controller;

import com.team1.cityfarm.portone.PortoneWebhookDto;
import com.team1.cityfarm.service.PortoneWebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/webhooks/portone")
@RequiredArgsConstructor
public class PortoneWebhookController {

    private final PortoneWebhookService portoneWebhookService;

    /**
     * [PortOne Webhook 수신 엔드포인트]
     * PortOne 결제/자동결제 비동기 결과 수신 (사용자 인증X)
     */
    @PostMapping
    public ResponseEntity<String> handleWebhook(
            @RequestHeader(value = "x-portone-signature", required = false) String signature,
            @RequestBody PortoneWebhookDto webhookDto
    ) {
        log.info("[PortOne Webhook Received] type: {}, paymentId: {}",
                webhookDto.getType(),
                webhookDto.getData() != null ? webhookDto.getData().getPaymentId() : "N/A");

        portoneWebhookService.processWebhook(webhookDto);

        // PortOne 서버가 재시도를 멈출 수 있도록 정상 수신(200 OK) 응답
        return ResponseEntity.ok("OK");
    }
}