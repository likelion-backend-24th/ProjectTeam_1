package com.team1.cityfarm.portone;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PortoneWebhookDto {

    private String type;         // 예: "Transaction.Paid", "Transaction.Failed", "Transaction.Cancelled"
    private String timestamp;
    private WebhookData data;

    @Getter
    @NoArgsConstructor
    public static class WebhookData {
        private String paymentId;
        private String transactionId;
        private String storeId;
        private String billingKey;
    }
}