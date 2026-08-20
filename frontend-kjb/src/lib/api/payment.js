import { apiRequest } from "./client";

// PortOne 결제창에서 반환된 결과는 그 자체로 성공을 의미하지 않는다.
// 반드시 이 verify 호출을 통해 백엔드가 PortOne API로 최종 검증한 결과를 신뢰한다.
// payload 예: { orderId, paymentId }
export function verifyPayment(payload) {
  return apiRequest("/api/payments/verify", { method: "POST", body: payload });
}

// 전체 취소만 지원한다(부분 환불 없음). 취소 가능 여부의 최종 판단은 백엔드가 한다.
export function cancelPayment(paymentId, payload) {
  return apiRequest(`/api/payments/${paymentId}/cancel`, { method: "POST", body: payload });
}

// TODO(backend): 환불 가능 여부 조회 API 경로가 확정되지 않았다.
// 예상 응답 형태: { refundable: boolean, reason: string | null }
// 프론트는 이 응답을 그대로 표시할 뿐, 현재 시각 기준으로 24시간 경과 여부 등을
// 직접 계산해서 최종 판단하지 않는다.
export function getPaymentRefundEligibility(paymentId) {
  return apiRequest(`/api/payments/${paymentId}/refund-eligibility`);
}
