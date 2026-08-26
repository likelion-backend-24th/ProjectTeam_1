import { apiRequest } from "./client";

// payload 예: { orderType: "GENERAL", classId }.
// 주문 생성 결과(주문번호, 결제 금액 등)는 전부 백엔드 응답을 그대로 신뢰한다.
export function createOrder(payload) {
  return apiRequest("/api/orders", { method: "POST", body: payload });
}

export function getOrder(orderId, signal) {
  return apiRequest(`/api/orders/${orderId}`, { signal });
}

// 결제창을 열고 결제를 완료하지 않은(PENDING) 주문을 즉시 취소한다.
// 아직 결제된 적이 없으므로 /api/payments/{paymentId}/cancel(환불)과는 다른 경로다.
export function cancelPendingOrder(orderId) {
  return apiRequest(`/api/orders/${orderId}/cancel`, { method: "POST" });
}

// TODO(backend): 결제 내역(주문 목록) 조회 API 경로가 요구사항 문서에 명시되지 않았다.
// 기존 컨벤션(GET /api/{리소스})을 따른 추정 경로이며, 계약이 확정되면 경로만 맞추면 된다.
export function getMyOrders({ page = 0, size = 10 } = {}) {
  return apiRequest("/api/orders/me", { query: { page, size } });
}
