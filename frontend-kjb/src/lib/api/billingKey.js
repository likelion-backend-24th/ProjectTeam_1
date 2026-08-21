import { apiRequest } from "./client";

// BillingKeyController 실제 경로 기준. 카드번호 등 민감정보는 여기서 절대 다루지 않고,
// 항상 PortOne SDK가 반환하는 issueId/billingKey 식별자만 전달한다.
// 활성 빌링키가 없으면 백엔드가 404를 내려준다 — 호출부에서 그 경우를 정상 상태로 처리할 것.

export function getMyBillingKey() {
  return apiRequest("/api/billing-keys/me");
}

// 1) 프론트가 발급 의도(BillingKeyIssuanceIntent)를 먼저 생성 요청
// 2) 응답(issueId, customerId)을 PortOne SDK 카드 등록 호출에 그대로 전달
export function createBillingKeyIssuanceIntent() {
  return apiRequest("/api/billing-keys/intent", { method: "POST" });
}

// PortOne SDK 카드 등록 완료 후, 발급된 billingKey를 백엔드에 전달해 PortOne API로
// 재조회·검증받는다. 두 값 다 요청 쿼리 파라미터로 전달한다(@RequestParam, JSON body 아님).
export function confirmBillingKeyIssuance({ issueId, billingKey }) {
  return apiRequest("/api/billing-keys/confirm", { method: "POST", query: { issueId, billingKey } });
}

// 카드 교체 없이 등록된 카드만 삭제(현재 사용자의 활성 빌링키 1건). 활성 구독에 다음 회차
// 예약이 걸려있으면 백엔드가 409로 거부한다.
export function deleteBillingKey(reason) {
  return apiRequest("/api/billing-keys/me", { method: "DELETE", query: reason ? { reason } : undefined });
}
