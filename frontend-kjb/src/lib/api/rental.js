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