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
    body: payload,
  });
}

// 현재 비밀번호 확인 API 호출
export function checkPassword(payload) {
  return apiRequest("/api/profile/check-password", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: payload,
  });
}

// 새 비밀번호 변경 API 호출
export function updatePassword(payload) {
  return apiRequest("/api/profile/password", {
    method: "PATCH",
    headers: {
      "Content-Type": "application/json",
    },
    body: payload,
  });
}

// 호스트 승격
export function promoteToHost(payload) {
  return apiRequest("/api/profile/host-promotion", { method: "POST", body: payload });
}