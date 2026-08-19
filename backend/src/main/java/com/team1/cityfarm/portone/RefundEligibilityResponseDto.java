package com.team1.cityfarm.portone;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class RefundEligibilityResponseDto {
    private boolean refundable;
    private String reason; // null or "REFUND_DEADLINE_EXCEEDED", "PASS_ALREADY_USED", "ALREADY_CANCELLED"

    public static RefundEligibilityResponseDto ok() {
        return new RefundEligibilityResponseDto(true, null);
    }

    public static RefundEligibilityResponseDto fail(String reason) {
        return new RefundEligibilityResponseDto(false, reason);
    }
}