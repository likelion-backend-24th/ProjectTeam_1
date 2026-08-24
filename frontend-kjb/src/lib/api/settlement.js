import { apiRequest } from "./client";

/**
 * [호스트] 내 정산 내역 목록 조회하기
 */
export function getHostSettlements() {
  return apiRequest("/api/host/settlements");
}