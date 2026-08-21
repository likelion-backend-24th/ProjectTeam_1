package com.team1.cityfarm.portone;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team1.cityfarm.global.exception.CustomError;
import com.team1.cityfarm.global.exception.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * PortonePaymentClient가 실제로 PortOne V2 API 스펙(portone-v2-openapi.json)대로
 * 요청을 내보내는지 검증한다. cancelSchedule/cancelScheduleByBillingKey/deleteBillingKey는
 * "body로 보내던 걸 query parameter로 고친" 지점이라 회귀 방지 가치가 특히 크다.
 * MockRestServiceServer로 실제 네트워크 없이 요청 형태만 가로채서 검증한다.
 */
class PortonePaymentClientTest {

    private PortonePaymentClient client;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        // 생성자는 내부에서 RestClient를 직접 만들어버려서 밖에서 주입할 방법이 없다.
        // 프로덕션 코드는 안 건드리고, 생성 후 리플렉션으로 restClient 필드만
        // MockRestServiceServer에 바인딩된 것으로 교체한다.
        client = new PortonePaymentClient("test-secret", "https://api.portone.io", "https://test.example.com/api/webhooks/portone", new ObjectMapper());

        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.portone.io");
        mockServer = MockRestServiceServer.bindTo(builder).build();
        ReflectionTestUtils.setField(client, "restClient", builder.build());
    }

    @Test
    void cancelPayment_reason은_JSON_바디로_전송된다() {
        mockServer.expect(requestTo("https://api.portone.io/payments/pay_1/cancel"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.reason").value("환불 사유"))
                .andRespond(withSuccess());

        client.cancelPayment("pay_1", "환불 사유");

        mockServer.verify();
    }

    @Test
    void payWithBillingKey_바디_형식과_PAID_아니면_예외() {
        mockServer.expect(requestTo("https://api.portone.io/payments/pay_2/billing-key"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.billingKey").value("bk_1"))
                .andExpect(jsonPath("$.amount.total").value(30000))
                .andExpect(jsonPath("$.customer.id").value("42"))
                .andExpect(jsonPath("$.noticeUrls[0]").value("https://test.example.com/api/webhooks/portone"))
                .andRespond(withSuccess("{\"id\":\"pay_2\",\"status\":\"FAILED\"}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() ->
                client.payWithBillingKey("bk_1", "pay_2", "BASIC 정기구독", 30000, 42L))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getCustomError())
                        .isEqualTo(CustomError.PORTONE_BILLING_FAILED));

        mockServer.verify();
    }

    @Test
    void payWithBillingKey_PAID면_정상_반환() {
        mockServer.expect(requestTo("https://api.portone.io/payments/pay_3/billing-key"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"id\":\"pay_3\",\"status\":\"PAID\"}", MediaType.APPLICATION_JSON));

        PortonePaymentResponseDto response = client.payWithBillingKey("bk_1", "pay_3", "BASIC 정기구독", 30000, 42L);

        assertThat(response.getStatus()).isEqualTo("PAID");
        mockServer.verify();
    }

    @Test
    void schedulePayment_바디_형식() {
        mockServer.expect(requestTo("https://api.portone.io/payments/pay_4/schedule"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.payment.billingKey").value("bk_1"))
                .andExpect(jsonPath("$.payment.amount.total").value(30000))
                .andExpect(jsonPath("$.payment.noticeUrls[0]").value("https://test.example.com/api/webhooks/portone"))
                .andExpect(jsonPath("$.timeToPay").exists())
                .andRespond(withSuccess("{\"schedule\":{\"id\":\"sch_1\",\"status\":\"SCHEDULED\"}}", MediaType.APPLICATION_JSON));

        PortonePaymentScheduleResponseDto response = client.schedulePayment(
                "bk_1", "pay_4", "BASIC 정기구독", 30000, 42L, LocalDateTime.now().plusMonths(1));

        assertThat(response.getSchedule().getId()).isEqualTo("sch_1");
        mockServer.verify();
    }

    @Test
    void cancelSchedule는_body가_아니라_requestBody_쿼리파라미터로_전송된다() {
        mockServer.expect(requestTo(
                        "https://api.portone.io/payment-schedules?requestBody=%7B%22scheduleIds%22%3A%5B%22sch_1%22%5D%7D"))
                .andExpect(method(HttpMethod.DELETE))
                .andExpect(content().string("")) // 진짜 HTTP 바디는 비어있어야 함(예전엔 여기 JSON이 실렸었음)
                .andRespond(withSuccess());

        client.cancelSchedule("sch_1");

        mockServer.verify();
    }

    @Test
    void cancelScheduleByBillingKey도_requestBody_쿼리파라미터로_전송된다() {
        mockServer.expect(requestTo(
                        "https://api.portone.io/payment-schedules?requestBody=%7B%22billingKey%22%3A%22bk_1%22%7D"))
                .andExpect(method(HttpMethod.DELETE))
                .andExpect(content().string(""))
                .andRespond(withSuccess());

        client.cancelScheduleByBillingKey("bk_1");

        mockServer.verify();
    }

    @Test
    void deleteBillingKey는_reason이_body가_아니라_쿼리파라미터로_전송된다() {
        mockServer.expect(requestTo("https://api.portone.io/billing-keys/bk_1?reason=%EC%B9%B4%EB%93%9C%20%EC%82%AD%EC%A0%9C"))
                .andExpect(method(HttpMethod.DELETE))
                .andExpect(content().string(""))
                .andRespond(withSuccess());

        client.deleteBillingKey("bk_1", "카드 삭제");

        mockServer.verify();
    }

    @Test
    void PortOne가_에러를_내려주면_CustomException으로_변환된다() {
        mockServer.expect(requestTo("https://api.portone.io/payments/pay_5/cancel"))
                .andRespond(withStatus(org.springframework.http.HttpStatus.BAD_REQUEST));

        assertThatThrownBy(() -> client.cancelPayment("pay_5", "사유"))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getCustomError())
                        .isEqualTo(CustomError.PORTONE_CANCEL_FAILED));
    }
}
