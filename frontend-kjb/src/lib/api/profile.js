import { apiRequest } from "./client";

export function getMyProfile() {
  return apiRequest("/api/profile");
}
