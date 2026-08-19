// 백엔드 enum과 1:1로 대응된다. 프론트에서 임의로 상태값을 추가하지 않는다.
// (backend: com.team1.cityfarm.entity.*Status / SubscriptionPlanType)

export const SubscriptionStatus = {
  ACTIVE: "ACTIVE",
  CANCELLED: "CANCELLED",
  EXPIRED: "EXPIRED",
};

export const SUBSCRIPTION_STATUS_LABEL = {
  ACTIVE: "구독중",
  CANCELLED: "해지됨",
  EXPIRED: "만료됨",
};

export const OrderStatus = {
  PENDING: "PENDING",
  PAID: "PAID",
  CANCELLED: "CANCELLED",
  FAILED: "FAILED",
};

export const ORDER_STATUS_LABEL = {
  PENDING: "결제 대기",
  PAID: "결제 완료",
  CANCELLED: "취소됨",
  FAILED: "결제 실패",
};

export const PaymentStatus = {
  PENDING: "PENDING",
  PAID: "PAID",
  FAILED: "FAILED",
  CANCELLED: "CANCELLED",
};

export const PAYMENT_STATUS_LABEL = {
  PENDING: "결제 대기",
  PAID: "결제 완료",
  FAILED: "결제 실패",
  CANCELLED: "결제 취소",
};

// 백엔드 ScheduleStatus는 PROCESSING을 포함한다 (문서상 스펙보다 백엔드 enum이 우선).
export const ScheduleStatus = {
  SCHEDULED: "SCHEDULED",
  PROCESSING: "PROCESSING",
  PAID: "PAID",
  FAILED: "FAILED",
  CANCELLED: "CANCELLED",
};

export const SCHEDULE_STATUS_LABEL = {
  SCHEDULED: "예약됨",
  PROCESSING: "처리중",
  PAID: "결제 완료",
  FAILED: "결제 실패",
  CANCELLED: "취소됨",
};

export const PassStatus = {
  ACTIVE: "ACTIVE",
  EXHAUSTED: "EXHAUSTED",
  EXPIRED: "EXPIRED",
};

export const PASS_STATUS_LABEL = {
  ACTIVE: "사용 가능",
  EXHAUSTED: "모두 사용함",
  EXPIRED: "기간 만료",
};

export const BillingKeyStatus = {
  ACTIVE: "ACTIVE",
  EXPIRED: "EXPIRED",
  REVOKED: "REVOKED",
};

export const BILLING_KEY_STATUS_LABEL = {
  ACTIVE: "등록됨",
  EXPIRED: "만료됨",
  REVOKED: "삭제됨",
};

export const BillingKeyIssuanceStatus = {
  PENDING: "PENDING",
  COMPLETED: "COMPLETED",
  FAILED: "FAILED",
  EXPIRED: "EXPIRED",
};

export const SubscriptionPlanType = {
  BASIC: "BASIC",
};

export const SUBSCRIPTION_PLAN_LABEL = {
  BASIC: "BASIC",
};

// 구독 플랜 소개(마케팅 표시)용 상수. 실제 결제 금액은 절대 이 값을 기준으로 계산하지 않고,
// 항상 백엔드가 주문/구독 생성 응답으로 내려주는 금액을 그대로 표시한다.
// TODO(backend): 플랜 가격/수강권 개수를 응답으로 내려주는 API가 생기면 이 상수는 제거하고 API 값으로 대체.
export const BASIC_PLAN = {
  planType: "BASIC",
  name: "BASIC",
  monthlyPrice: 30000,
  monthlyPassCount: 3,
};
