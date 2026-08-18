import { apiRequest } from "./client";

export function getMyProfile() {
  return apiRequest("/api/profile");
}

export function updateProfile(payload) {
  return apiRequest("/api/profile", {
    method: "PATCH", // 서버 규격에 따라 PUT일 경우 수정해주세요
    body: JSON.stringify(payload),
  });
}