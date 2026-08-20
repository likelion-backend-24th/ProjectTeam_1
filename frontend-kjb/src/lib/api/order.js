import { apiRequest } from "./client";

// 요구사항 12절에 명시된 예상 경로. OrderController가 아직 없어 연결 전까지 404가 날 수 있다.

// payload 예: { orderType: "GENERAL", classId } 형태를 상정하나,
// OneDayClass/ClassEnrollment 연동은 별도 작업으로 미룬 상태라 classId는 아직 사용하지 않는다.
// 주문 생성 결과(주문번호, 결제 금액 등)는 전부 백엔드 응답을 그대로 신뢰한다.
export function createOrder(payload) {
  return apiRequest("/api/orders", { method: "POST", body: payload });
}

export function getOrder(orderId, signal) {
  return apiRequest(`/api/orders/${orderId}`, { signal });
}

// TODO(backend): 결제 내역(주문 목록) 조회 API 경로가 요구사항 문서에 명시되지 않았다.
// 기존 컨벤션(GET /api/{리소스})을 따른 추정 경로이며, 계약이 확정되면 경로만 맞추면 된다.
export function getMyOrders({ page = 0, size = 10 } = {}) {
  return apiRequest("/api/orders/me", { query: { page, size } });
}
