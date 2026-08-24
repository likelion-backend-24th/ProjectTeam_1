import { apiRequest } from "./client";

// OneDayClassController 실제 경로 기준.

export function getClassList({ page = 0, size = 10, sort = "date,asc" } = {}) {
  return apiRequest("/api/onedayclass", { query: { page, size, sort } });
}

export function getClassDetail(classId, { signal } = {}) {
  return apiRequest(`/api/onedayclass/${classId}`, { signal });
}

// 보유한 구독 수강권 1개를 차감해 결제 없이 즉시 신청을 확정한다.
export function enrollClassWithPass(classId) {
  return apiRequest(`/api/onedayclass/${classId}/enroll-with-pass`, { method: "POST" });
}

// 호스트 권한 계정만 호출 가능 (백엔드가 role 검증).
export function createOneDayClass(payload) {
  return apiRequest("/api/onedayclass", { method: "POST", body: payload });
}
