import { apiRequest } from "./client";

export function followUser(followingId) {
  return apiRequest("/api/follows", { method: "POST", body: { followingId } });
}

export function unfollowUser(followId) {
  return apiRequest(`/api/follows/${followId}`, { method: "DELETE" });
}

export function getFollowingList() {
  return apiRequest("/api/follows/following");
}

export function getFollowerList() {
  return apiRequest("/api/follows/followers");
}
