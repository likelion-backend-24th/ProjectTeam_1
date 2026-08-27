import { apiRequest } from "./client";

/**
 * [호스트] 내 정산 내역 목록 조회
 */
export function getHostSettlements() {
  return apiRequest("/api/host/settlements");
}

/**
 * [관리자] 전체 정산 내역 조회
 */
export function getAdminSettlements() {
  return apiRequest("/api/admin/settlements");
}

/**
 * [관리자] 정산 지급 완료 처리
 */
export function payoutSettlement(settlementId) {
  return apiRequest(`/api/admin/settlements/${settlementId}/payout`, {
    method: "PATCH",
  });
}