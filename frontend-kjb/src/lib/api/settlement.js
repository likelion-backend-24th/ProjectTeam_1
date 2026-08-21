import { apiRequest } from "./client";

/**
 * [호스트] 내 정산 내역 목록 조회
 */
export function getHostSettlements() {
  return apiRequest("/api/host/settlements");
}