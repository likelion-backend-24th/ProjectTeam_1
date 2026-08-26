import { apiRequest } from "./client";

// SubscriptionController 실제 경로 기준.

export function getMySubscription() {
  return apiRequest("/api/subscriptions/me");
}

// payload 예: { planType: "BASIC", billingKeyId }
// 금액은 프론트에서 계산하지 않는다. 백엔드가 planType을 기준으로 금액을 결정하고
// 생성된 구독/주문 정보를 응답으로 내려준다.
export function createSubscription(payload) {
  return apiRequest("/api/subscriptions", { method: "POST", body: payload });
}

// 구독 해지/환불 요청.
// 결제 후 24시간 이내 + 수강권 미사용이면 백엔드가 전액 환불 후 즉시 해지 처리하고,
// 그 외에는 cancel_at_period_end로 처리된다(현재 구독 기간 종료 시점에 해지, 그 전까지는 남은 수강권 사용 가능).
// 응답: { resultType: "REFUNDED" | "SCHEDULED_CANCEL", subscription, refundedAmount }
export function cancelSubscription(subscriptionId) {
  return apiRequest(`/api/subscriptions/${subscriptionId}/cancel`, { method: "POST" });
}

// 구독 하나당 활성 수강권은 항상 1건이라 백엔드가 배열이 아닌 단건 객체로 응답한다.
export function getSubscriptionPass(subscriptionId) {
  return apiRequest(`/api/subscriptions/${subscriptionId}/passes`);
}

export function getSubscriptionPassUsages(passId) {
  return apiRequest(`/api/subscriptions/passes/${passId}/usages`);
}
