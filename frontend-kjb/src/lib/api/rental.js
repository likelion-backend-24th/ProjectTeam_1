import { apiRequest } from "./client";

export function applyRental(farmId, payload) {
  return apiRequest(`/api/farms/${farmId}/rent`, { method: "POST", body: payload ?? {} });
}

export function cancelRental(rentalId) {
  return apiRequest(`/api/rentals/${rentalId}`, { method: "DELETE" });
}

export function getHostRentals({ page = 0, size = 10 } = {}) {
  return apiRequest("/api/host/rentals", { query: { page, size } });
}

// 내가 신청해서 확정된 임대 목록
export function getMyRentals({ page = 0, size = 10 } = {}) {
  return apiRequest("/api/rentals/me", { query: { page, size } });
}

// 밭 임대 결제 주문 생성
export function createFarmRentalOrder(farmId, description){
  return apiRequest("/api/orders/farm-rental",{
    method: "POST",
    body: { farmId, description: description || null },
  })
}