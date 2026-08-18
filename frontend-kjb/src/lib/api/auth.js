import { apiRequest } from "./client";

export function signup(payload) {
  return apiRequest("/api/auth/signup", { method: "POST", body: payload });
}

export function login(payload) {
  return apiRequest("/api/auth/login", { method: "POST", body: payload });
}

export function logout() {
  return apiRequest("/api/auth/logout", { method: "POST" });
}

export function withdraw() {
  return apiRequest("/api/auth/withdraw", { method: "DELETE" });
}
