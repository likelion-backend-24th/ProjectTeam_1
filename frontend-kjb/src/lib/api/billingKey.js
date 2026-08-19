import { apiRequest } from "./client";

// TODO(backend): 빌링키 관련 API는 요구사항 문서(12절)에도 경로가 명시되지 않았다.
// BillingKeyController가 아직 없으므로 아래 경로는 전부 추정치이며,
// 백엔드 계약이 확정되면 경로만 맞춰 수정하면 된다. 카드번호 등 민감정보는
// 여기서 절대 다루지 않고, 항상 PortOne SDK가 반환하는 issueId/billingKey 식별자만 전달한다.

export function getMyBillingKey() {
  return apiRequest("/api/billing-keys/me");
}

// 1) 프론트가 발급 의도(BillingKeyIssuanceIntent)를 먼저 생성 요청
// 2) 응답으로 받은 issueId를 PortOne SDK 카드 등록 호출에 그대로 전달
export function createBillingKeyIssuanceIntent() {
  return apiRequest("/api/billing-keys/issuance-intents", { method: "POST" });
}

// PortOne SDK 카드 등록 완료 후, 발급 결과를 백엔드에 통지하여 최종 검증받는다.
// (실제로는 Webhook으로도 백엔드가 처리하므로, 이 호출은 화면 갱신용 보조 확인 성격.)
export function confirmBillingKeyIssuance(issueId) {
  return apiRequest(`/api/billing-keys/issuance-intents/${issueId}/confirm`, { method: "POST" });
}

export function deleteBillingKey(billingKeyId) {
  return apiRequest(`/api/billing-keys/${billingKeyId}`, { method: "DELETE" });
}
