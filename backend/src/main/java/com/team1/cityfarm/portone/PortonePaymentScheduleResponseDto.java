package com.team1.cityfarm.portone;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * PortOne V2 [POST /payments/{paymentId}/schedule] 결제 예약 생성 응답
 */
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PortonePaymentScheduleResponseDto {

    private Schedule schedule;

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Schedule {
        private String id;      // PortOne 결제 예약 식별자
        private String status;  // SCHEDULED, STARTED, SUCCEEDED, FAILED, REVOKED 등
    }
}
