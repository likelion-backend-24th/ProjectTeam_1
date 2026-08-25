import { apiRequest, API_BASE_URL } from "./client";

export function getFarms({ page = 0, size = 10, sort } = {}) {
  return apiRequest("/api/farms", { query: { page, size, sort } });
}

export function getFarm(farmId, signal) {
  return apiRequest(`/api/farms/${farmId}`, { signal });
}

export function createFarm(
  { title, location, locationAddress, area, monthlyRent, rentalMonths, description },
  images,
) {
  const formData = new FormData();
  formData.append(
    "request",
    new Blob(
      [JSON.stringify({ title, location, locationAddress, area, monthlyRent, rentalMonths, description })],
      { type: "application/json" },
    ),
  );
  (images ?? []).forEach((file) => formData.append("images", file));
  return apiRequest("/api/farms", { method: "POST", body: formData });
}

export function updateFarm(farmId, { title, area, monthlyRent, rentalMonths, description }, images){
  const formData = new FormData();
  formData.append(
    "request",
    new Blob(
      [JSON.stringify({ title, area, monthlyRent, rentalMonths, description })],
      { type: "application/json" },
    ),
  );
  (images ?? []).forEach((file) => formData.append("images", file));
  return apiRequest(`/api/farms/${farmId}`, { method: "PUT", body: formData });
}

export function getMyFarms() {
  return apiRequest("/api/host/farms");
}

export function resolveFarmImageUrl(path) {
  return path ? `${API_BASE_URL}${path}` : null;
}