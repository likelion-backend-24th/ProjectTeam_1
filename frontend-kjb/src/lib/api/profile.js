import { apiRequest } from "./client";

export function getMyProfile() {
  return apiRequest("/api/profile");
}

export function updateProfile(payload) {
  return apiRequest("/api/profile", {
    method: "PATCH",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(payload),
  });
}