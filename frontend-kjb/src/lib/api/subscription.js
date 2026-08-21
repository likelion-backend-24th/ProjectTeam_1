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

// 구독 해지: 즉시 해지가 아니라 cancel_at_period_end 방식.
// 현재 구독 기간 종료 시점에 해지되며, 그 전까지는 남은 수강권을 사용할 수 있다.
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
