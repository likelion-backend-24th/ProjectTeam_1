import { apiRequest } from "./client";

export function getFeed({ page = 0, size = 10 } = {}) {
  return apiRequest("/api/feed", { query: { page, size } });
}
