import { apiRequest } from "./client";

export function getAllUsers({ page = 0, size = 10 } = {}) {
  return apiRequest("/api/admin/users", { query: { page, size } });
}

export function patchUser(userId, payload) {
  return apiRequest(`/api/admin/users/${userId}`, { method: "PATCH", body: payload });
}
