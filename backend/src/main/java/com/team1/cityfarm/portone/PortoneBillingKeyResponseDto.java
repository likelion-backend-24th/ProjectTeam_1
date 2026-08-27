package com.team1.cityfarm.portone;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * PortOne V2 [GET /billing-keys/{billingKey}] 빌링키 단건 조회 응답
 */
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PortoneBillingKeyResponseDto {

    private String status;      // ISSUED, DELETE_READY, DELETED 등
    private String billingKey;  // PortOne이 발급한 빌링키 값 (조회 요청 값과 동일해야 함)
    private String issuedAt;    // 발급 일시 (ISO-8601)
}
