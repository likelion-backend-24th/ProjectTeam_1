package com.team1.cityfarm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team1.cityfarm.portone.PortonePaymentClient;
import com.team1.cityfarm.repository.OrderRepository;
import com.team1.cityfarm.repository.WebhookInboxRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.mockito.Mockito.*;

/**
 * DB/Spring 컨텍스트 없이 PortoneWebhookService의 서명 검증 로직만 검증하는 단위 테스트.
 * 실제 PortOne 시크릿은 쓰지 않고 테스트 전용 더미 시크릿으로 직접 서명해서 확인한다
 * (알고리즘 정합성만 확인하면 되므로 실제 값이 필요 없음).
 */
@ExtendWith(MockitoExtension.class)
class PortoneWebhookServiceSignatureTest {

    private static final String TEST_SECRET = "whsec_dGVzdC1zZWNyZXQtZm9yLXVuaXQtdGVzdA==";

    @Mock
    private WebhookInboxRepository webhookInboxRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private PortonePaymentClient portOnePaymentClient;
    @Mock
    private PaymentService paymentService;
    @Mock
    private SubscriptionService subscriptionService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private PortoneWebhookService newService() {
        PortoneWebhookService service = new PortoneWebhookService(
                webhookInboxRepository, orderRepository, portOnePaymentClient,
                paymentService, subscriptionService, objectMapper
        );
        ReflectionTestUtils.setField(service, "webhookSecret", TEST_SECRET);
        return service;
    }

    private String sign(String webhookId, String timestamp, String body) throws Exception {
        byte[] key = Base64.getDecoder().decode(TEST_SECRET.substring("whsec_".length()));
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        byte[] hash = mac.doFinal((webhookId + "." + timestamp + "." + body).getBytes(StandardCharsets.UTF_8));
        return "v1," + Base64.getEncoder().encodeToString(hash);
    }

    @Test
    void 유효한_서명이면_멱등성_체크까지_진행된다() throws Exception {
        String webhookId = "wh_test_1";
        String timestamp = "1700000000";
        String body = "{\"type\":\"Transaction.Paid\",\"data\":{\"paymentId\":\"test-payment-1\"}}";
        String signature = sign(webhookId, timestamp, body);

        // 서명 검증을 통과해야만 도달하는 지점에서 바로 종료시켜, 이후 비즈니스 로직은 테스트 범위 밖으로 둔다.
        when(webhookInboxRepository.existsByWebhookId(webhookId)).thenReturn(true);

        newService().processWebhook(webhookId, timestamp, signature, body);

        verify(webhookInboxRepository).existsByWebhookId(webhookId);
    }

    @Test
    void 서명이_틀리면_이후_로직이_전혀_실행되지_않는다() {
        String webhookId = "wh_test_2";
        String timestamp = "1700000000";
        String body = "{\"type\":\"Transaction.Paid\",\"data\":{\"paymentId\":\"test-payment-2\"}}";
        String wrongSignature = "v1," + Base64.getEncoder().encodeToString("garbage".getBytes(StandardCharsets.UTF_8));

        newService().processWebhook(webhookId, timestamp, wrongSignature, body);

        verifyNoInteractions(webhookInboxRepository, orderRepository, portOnePaymentClient, paymentService, subscriptionService);
    }

    @Test
    void body가_한글자라도_바뀌면_서명이_거부된다() throws Exception {
        String webhookId = "wh_test_3";
        String timestamp = "1700000000";
        String body = "{\"type\":\"Transaction.Paid\",\"data\":{\"paymentId\":\"test-payment-3\"}}";
        String signature = sign(webhookId, timestamp, body);
        String tamperedBody = body.replace("test-payment-3", "test-payment-999");

        newService().processWebhook(webhookId, timestamp, signature, tamperedBody);

        verifyNoInteractions(webhookInboxRepository);
    }
}
