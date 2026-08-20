# 2026-08-20 프론트(D파트) 확인 필요 사항 — 백엔드 카드 변경/구독 작업 관련

백엔드 쪽(A파트, 빌링키/구독)에서 카드 변경 시 예약결제 마이그레이션 작업을 하면서
PortOne 실제 API 응답 형식(`portone-v2-openapi.json`)에 맞춰 백엔드만 수정했습니다.
아래 항목은 프론트 쪽 확인/수정이 필요할 수 있는 것만 정리한 겁니다 — 나머지는 백엔드에서
이미 정합성 맞춰서 프론트 변경 없이 처리됩니다.

## 확인 불필요 (참고만)

- 구독 시작 요청(`POST /api/subscriptions`) 필드명이 프론트가 이미 보내고 있는
  `{ planType, billingKeyId }` 그대로 백엔드 계약과 맞춰졌습니다(기존엔 백엔드가
  `billingKey`라는 다른 이름을 기대하고 있어서 실제로는 항상 실패했을 가능성이 있었음).
  **프론트 코드 변경 필요 없습니다.**

## 확인/수정이 필요할 수 있는 것

1. **환불 불가 사유 라벨에 `UNSUPPORTED_ORDER_TYPE` 없음**
   `CancelPaymentButton.js`의 `REASON_LABEL`에 `REFUND_DEADLINE_EXCEEDED` / `PASS_ALREADY_USED` /
   `ALREADY_CANCELLED`만 있고, 이번에 추가된 `UNSUPPORTED_ORDER_TYPE`(구독 결제 건은 이 환불
   플로우로 취소 불가) 사유가 없습니다. 매핑 안 된 사유는 기본 문구("이 결제는 환불할 수 없어요")로
   대체되긴 하니 당장 깨지진 않지만, 사유를 명확히 보여주고 싶으면 라벨 추가 필요합니다.
   (현재 프론트는 GENERAL 주문에만 취소 버튼을 띄우고 있어서 실제로 이 사유를 볼 일은 거의 없음)

2. **카드 변경 시 "예약 마이그레이션 실패"에 대한 별도 안내 없음**
   구독 중에 카드를 바꾸다가 백엔드에서 예약 마이그레이션이 실패하면(PortOne 쪽 오류 등)
   전체 트랜잭션이 롤백되고 일반적인 `PORTONE_CANCEL_FAILED`/`PORTONE_BILLING_FAILED` 에러가
   내려갑니다. `BillingKeyCard.js`의 기존 catch-all 에러 토스트("카드 등록에 실패했어요.
   다시 시도해주세요.")로 커버는 되는데, 구체적으로 "구독 결제 예약 문제로 카드 변경이
   안 된다"는 걸 사용자에게 더 명확히 보여주고 싶으면 에러 코드별 메시지 분기를 추가하면 됩니다.
   필수는 아니고 UX 개선 옵션입니다.

3. **수강권 사용 내역(`PassUsageSection.js`) 실데이터 QA**
   이건 이전에도 안내드린 항목인데, 이번에 백엔드 API가 실제로 연결됐으니
   (`GET /api/subscriptions/passes/{passId}/usages`) 아직 실데이터로 확인 안 해보셨으면
   한 번 훑어봐주시면 좋겠습니다.
