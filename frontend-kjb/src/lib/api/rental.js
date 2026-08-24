import { apiRequest } from "./client";

export function applyRental(farmId, payload) {
  return apiRequest(`/api/farms/${farmId}/rent`, { method: "POST", body: payload ?? {} });
}