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

// 클래스 설명만 수정 가능 (제목/일정/장소/정원/가격은 수정 API 없음). 본인 클래스만 가능.
export function updateClassDescription(classId, description) {
  return apiRequest(`/api/onedayclass/${classId}`, { method: "PATCH", body: { description } });
}

// 클래스 취소: 신청자 전체 환불/수강권 복구까지 백엔드가 처리한다. 본인 클래스만 가능.
export function cancelOneDayClass(classId) {
  return apiRequest(`/api/onedayclass/${classId}/cancel`, { method: "POST" });
}

// 호스트 본인 클래스의 신청자 목록 조회.
export function getClassApplicants(classId) {
  return apiRequest(`/api/onedayclass/${classId}/applicants`);
}
