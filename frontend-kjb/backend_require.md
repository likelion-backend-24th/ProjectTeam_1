# 백엔드 요구사항 정리 (결제/구독 프론트 연동)

프론트(`frontend-kjb`)에 결제/구독/빌링키 화면과 API 레이어를 먼저 구현해두었다.
이 문서는 각 API 호출부가 어떤 파라미터를 보내고 어떤 응답 형태를 기대하는지,
그리고 프론트에 `TODO`로 남겨둔 부분을 정리한 것이다.

공통 규칙 (`src/lib/api/client.js`):
- 모든 응답은 `{ success, data, message?, code? }` 형태를 기대하며, 프론트는 `data`만 사용한다.
- 실패 시 HTTP status + `{ message, code }`를 기대한다 (`ApiError`로 매핑).
- 인증은 `Authorization: Bearer {accessToken}` 헤더.

상태 enum은 백엔드 엔티티의 enum 값을 그대로 사용했다 (`src/lib/constants/status.js`).
예: `ScheduleStatus`에 `PROCESSING`이 포함되어 있음을 반영함.

---

## 1. 구현 완료 — 요구사항 문서(12절)에 경로가 명시된 API

### `GET /api/subscriptions/me`
- 파일: `src/lib/api/subscription.js` → `getMySubscription()`
- 파라미터: 없음 (인증 토큰만)
- 기대 응답 (`data`):
  ```json
  {
    "id": 1,
    "planType": "BASIC",
    "status": "ACTIVE",
    "currentPeriodStart": "2026-08-18T00:00:00",
    "currentPeriodEnd": "2026-09-18T00:00:00",
    "cancelAtPeriodEnd": false
  }
  ```
- 구독 이력이 없는 사용자는 **404**를 기대함(프론트가 "구독 없음" 상태로 처리, 에러 배너를 띄우지 않음).
  404가 아닌 다른 방식(예: `data: null` + 200)으로 설계할 경우 프론트 분기 로직 수정 필요.

### `POST /api/subscriptions`
- 파일: `src/lib/api/subscription.js` → `createSubscription(payload)`
- 요청 바디: `{ planType: "BASIC", billingKeyId: number }`
  - 금액은 프론트가 계산하지 않는다. 백엔드가 `planType` 기준으로 결정.
- 기대 응답: 생성된 구독 객체 (위 `getMySubscription` 응답과 동일 형태)

### `POST /api/subscriptions/{id}/cancel`
- 파일: `src/lib/api/subscription.js` → `cancelSubscription(subscriptionId)`
- 파라미터: path의 `subscriptionId`, 바디 없음
- 동작: `cancel_at_period_end = true`로 전환 (즉시 해지 아님)
- 기대 응답: 갱신된 구독 객체 (`cancelAtPeriodEnd: true` 반영)

### `GET /api/subscriptions/{id}/passes`
- 파일: `src/lib/api/subscription.js` → `getSubscriptionPasses(subscriptionId)`
- 기대 응답: `SubscriptionPass[]` 배열. 프론트는 `passes[0]`(가장 최근/현재 유효 pass)만 사용한다.
  ```json
  [
    {
      "id": 10,
      "totalCount": 3,
      "remainingCount": 2,
      "validFrom": "2026-08-18T00:00:00",
      "validUntil": "2026-09-18T00:00:00",
      "status": "ACTIVE"
    }
  ]
  ```
  - 응답이 여러 개일 때 어떤 것을 "현재 pass"로 볼지(정렬 기준)는 백엔드와 합의 필요.
    현재 프론트는 배열의 첫 번째 요소를 그대로 사용한다.

### `POST /api/orders`
- 파일: `src/lib/api/order.js` → `createOrder(payload)`
- 요청 바디: 아직 확정 아님. 문서 12절 스펙만 존재.
  - OneDayClass/ClassEnrollment 연동을 이번 작업에서 제외했기 때문에, 실제 호출부(`createOrder` 사용처)는
    아직 어디서도 호출하지 않는다. 클래스 신청 기능을 붙일 때 `{ orderType: "GENERAL", classId, ... }`
    형태로 payload를 확정해야 함.
- 기대 응답: 생성된 주문 객체 (아래 `getOrder` 응답과 동일 형태)

### `GET /api/orders/{orderId}`
- 파일: `src/lib/api/order.js` → `getOrder(orderId, signal)`
- 사용처: `/payment/checkout`(결제 전 확인), `/payment/[orderId]`(결제 결과/상세)
- **기대 응답 형태 (프론트가 실제로 참조하는 필드)**:
  ```json
  {
    "id": 123,
    "merchantOrderId": "ORD-20260818-0001",
    "orderType": "GENERAL",
    "orderStatus": "PAID",
    "amount": 30000,
    "originalAmount": 30000,
    "title": "원데이클래스 이름 또는 BASIC 구독",
    "scheduledAt": "2026-08-25T14:00:00",
    "createdAt": "2026-08-18T10:00:00",
    "payment": {
      "id": 55,
      "status": "PAID",
      "payMethod": "CARD",
      "approvedAt": "2026-08-18T10:00:10",
      "cancelledAt": null,
      "cancelReason": null
    }
  }
  ```
  - `title`, `scheduledAt`, `originalAmount`는 문서 12절에 없던 필드로, 화면 표시를 위해 프론트가
    임의로 기대한 이름이다. 백엔드 응답 필드명이 다르면 이 세 개는 매핑 조정이 필요하다.
    (`title`/`scheduledAt`이 없으면 프론트는 "클래스 결제"/"BASIC 구독"으로 대체 표시하도록
    fallback 처리는 이미 해둠.)
  - `payment`가 **중첩 객체로 함께 내려오는 것을 전제**로 설계했다. 별도 엔드포인트로 분리된다면
    `/payment/[orderId]/page.js`, `/payment/checkout/page.js`의 `order.payment` 참조 부분을
    별도 API 호출로 바꿔야 한다.

### `POST /api/payments/verify`
- 파일: `src/lib/api/payment.js` → `verifyPayment(payload)`
- 요청 바디: `{ orderId, paymentId }` (PortOne 결제창이 반환한 `paymentId`)
- 기대 응답: 검증된 결제/주문 상태. 현재 프론트는 응답 값을 직접 쓰지 않고,
  성공(200) 시 `/payment/{orderId}`로 이동해 `getOrder`로 최종 상태를 다시 조회한다.
  → 필요하다면 검증 응답에 최신 order 객체를 바로 실어줘도 되고, 프론트가 재조회하는 현재
  방식을 유지해도 무방하다.

### `POST /api/payments/{paymentId}/cancel`
- 파일: `src/lib/api/payment.js` → `cancelPayment(paymentId, payload)`
- 요청 바디: 현재 `{}` (빈 객체) 전송 중. 취소 사유 등을 받을 필드가 필요하면 추가 협의 필요.
- 동작: **전체 취소만 지원** (부분 환불 없음)
- 기대 응답: 취소된 payment/order 상태

---

## 2. 구현은 해뒀지만 경로가 문서에 없어 추정 경로로 작성함 (`TODO(backend)`)

아래는 전부 함수 시그니처와 호출부는 완성돼 있고, `apiRequest` 경로만 실제 백엔드와
맞추면 되는 상태다. 지금 상태로는 컨트롤러가 없어 전부 404가 난다.

### `GET /api/orders/me` — 결제 내역(주문 목록)
- 파일: `src/lib/api/order.js` → `getMyOrders({ page, size })`
- 사용처: `/profile/payments`
- 파라미터: 쿼리 `page`(0-base), `size`
- 기대 응답: 페이지네이션 객체 또는 배열 모두 대응하도록 짜뒀음
  (`res?.content ?? res ?? []`). 게시판 목록 API(`GET /api/board`)와 동일한
  `{ content: [...], totalPages, ... }` 형태를 권장.
- 목록 아이템에 필요한 필드는 위 `getOrder` 응답과 동일 (`orderType`, `orderStatus`, `amount`, `title`, `createdAt`).

### `GET /api/payments/{paymentId}/refund-eligibility` — 환불 가능 여부
- 파일: `src/lib/api/payment.js` → `getPaymentRefundEligibility(paymentId)`
- 사용처: `components/payment/CancelPaymentButton.js`
- 파라미터: path의 `paymentId`
- **기대 응답 (요구사항 문서 10절 스펙 그대로)**:
  ```json
  { "refundable": true, "reason": null }
  ```
  또는
  ```json
  { "refundable": false, "reason": "REFUND_DEADLINE_EXCEEDED" }
  ```
  - 프론트는 `reason` 문자열을 아래 두 코드에 대해서만 한글 메시지로 매핑해뒀다.
    다른 코드가 추가되면 `CancelPaymentButton.js`의 `REASON_LABEL`에 추가해야 함.
    - `REFUND_DEADLINE_EXCEEDED` → "클래스 신청 후 24시간이 지나 환불할 수 없습니다."
    - `PASS_ALREADY_USED` → "이미 수강권을 사용하여 환불할 수 없습니다."
    - `ALREADY_CANCELLED` → "이미 취소된 결제예요."
    - 매핑에 없는 코드가 오면 "이 결제는 환불할 수 없어요."로 대체 표시.
  - 프론트는 현재 시각 기준 24시간 경과 여부, 수강권 사용 여부 등을 **직접 계산하지 않는다.**
    반드시 이 API의 `refundable` 값을 그대로 신뢰한다.

### `GET /api/billing-keys/me` — 내 등록 카드 조회
- 파일: `src/lib/api/billingKey.js` → `getMyBillingKey()`
- 사용처: `/subscription/billing`
- 기대 응답: `{ id, status: "ACTIVE" | "EXPIRED" | "REVOKED", issuedAt, expiredAt }` 또는 미등록 시 404/`null`
  (프론트는 실패 시 "등록된 결제수단 없음"으로 처리)

### `POST /api/billing-keys/issuance-intents` — 빌링키 발급 의도 생성
- 파일: `src/lib/api/billingKey.js` → `createBillingKeyIssuanceIntent()`
- 파라미터: 없음 (바디 없음)
- 기대 응답: `{ issueId: string, expiresAt: string, ... }`
  → 이 `issueId`를 PortOne SDK `requestIssueBillingKey` 호출에 그대로 전달한다.

### `POST /api/billing-keys/issuance-intents/{issueId}/confirm` — 발급 완료 통지
- 파일: `src/lib/api/billingKey.js` → `confirmBillingKeyIssuance(issueId)`
- PortOne SDK 발급 성공 콜백 이후 프론트가 보조로 호출 (최종 처리는 Webhook 기준).
- 기대 응답: 등록된 `BillingKey` 객체 (`getMyBillingKey` 응답과 동일 형태)

### `DELETE /api/billing-keys/{billingKeyId}` — 카드 삭제/변경
- 파일: `src/lib/api/billingKey.js` → `deleteBillingKey(billingKeyId)`
- 현재 화면(`BillingKeyCard`)에서는 아직 호출부를 연결하지 않음(카드 "변경" 버튼은
  재등록 플로우만 태움). 카드 삭제 UX가 필요해지면 연결 필요.

---

## 3. 아예 엔드포인트를 만들지 않고 TODO로만 남긴 것

### 수강권 사용 내역 (`SubscriptionPassUsage`)
- 파일: `src/components/subscription/PassUsageSection.js`, `src/lib/api/subscription.js` → `getSubscriptionPassUsages(passId)`
- 요구사항 문서(8절)에서 "API가 없으면 임의로 만들지 말라"고 명시되어 있어,
  **경로를 추정하지 않고** 함수가 즉시 `Promise.reject(new Error("NOT_IMPLEMENTED..."))`를 반환하도록만 해뒀다.
- 화면에는 "수강권 사용 내역 기능은 준비 중이에요." 안내만 표시됨.
- **필요 작업**: API 계약이 나오면 `getSubscriptionPassUsages`의 본문만
  `apiRequest(...)` 호출로 교체하면 된다. 기대하는 아이템 형태(컴포넌트가 이미 참조 중):
  ```json
  { "id": 1, "className": "원데이클래스 A", "classDate": "2026-08-25", "usedAt": "2026-08-18T10:00:00" }
  ```

### PortOne 결제 요청 (`window.PortOne.requestPayment`)
- 파일: `src/lib/portone/client.js` → `requestPayment({ order })`
- 현재 SDK 스크립트(`https://cdn.portone.io/v2/browser-sdk.js`)는 로드하지만,
  실제 결제창 호출부는 `throw new Error("NOT_IMPLEMENTED: ...")`로 막아뒀다.
- **필요 정보**: `NEXT_PUBLIC_PORTONE_STORE_ID`, `NEXT_PUBLIC_PORTONE_CHANNEL_KEY`
  (PortOne 콘솔에서 발급하는 공개 식별자 — API Secret 아님), 그리고 결제 요청에
  실어야 할 정확한 파라미터명(현재는 `paymentId`/`orderName`/`totalAmount`/`currency`/`payMethod`로 가정).
- 연결 후에도 **결제창 반환값만으로 성공을 확정하지 않고**, 반드시 `verifyPayment()`로
  백엔드 검증을 거치는 흐름은 이미 코드에 고정되어 있음.

### PortOne 빌링키 발급 요청 (`window.PortOne.requestIssueBillingKey`)
- 파일: `src/lib/portone/client.js` → `requestIssueBillingKey({ intent })`
- 위와 동일하게 `NOT_IMPLEMENTED`로 막아뒀고, storeId/channelKey/issueId 파라미터
  확정 후 연결 필요.

### 구독 플랜 가격/수강권 개수 표시
- 파일: `src/lib/constants/status.js` → `BASIC_PLAN` 상수 (`monthlyPrice: 30000`, `monthlyPassCount: 3`)
- 마케팅 카드(`PlanCard`)에 표시하는 값이며, **실제 결제 금액 계산에는 전혀 쓰이지 않는다**
  (결제 금액은 항상 `createSubscription`/`getOrder` 응답을 그대로 표시).
- 플랜 가격을 응답으로 내려주는 API가 생기면 이 하드코딩 상수는 제거하고 API 값으로 교체해야 함.

### OneDayClass / ClassEnrollment 연동
- 이번 작업 범위에서 명시적으로 제외됨(추후 별도 설계 예정).
- `POST /api/enrollments`, `GET /api/enrollments/{id}` (문서 12절)는 아직 프론트에
  아예 만들지 않았다. `/payment/checkout`은 `orderId` 쿼리 파라미터만 받는 범용 구조로
  만들어뒀기 때문에, 클래스 상세 페이지가 생기면 "신청 → `createOrder` 호출 →
  `/payment/checkout?orderId={id}`로 이동" 흐름으로 그대로 연결하면 된다.

---

## 4. 참고 — 프론트가 신뢰하지 않는 것 (설계상 의도)

- 결제 금액: 항상 백엔드 응답값을 표시만 함 (프론트 재계산 없음)
- 결제 성공 여부: PortOne 결제창 결과 자체를 최종 상태로 쓰지 않고, `verifyPayment` 응답 이후
  `getOrder` 재조회로만 화면에 반영
- 환불 가능 여부: `refundable`/`reason`을 그대로 표시, 24시간 경과 등은 프론트에서 계산 안 함
- Webhook 처리: 프론트 코드 어디에도 없음 (백엔드 전담)
