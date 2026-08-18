import { apiRequest } from "./client";

export function getBoards({ type, keyword, page = 0, size = 10, sort } = {}) {
  return apiRequest("/api/board", { query: { type, keyword, page, size, sort } });
}

export function getBoard(boardId, signal) {
  return apiRequest(`/api/board/${boardId}`, { signal });
}

export function createBoard(payload) {
  return apiRequest("/api/board", { method: "POST", body: payload });
}

export function updateBoard(boardId, payload) {
  return apiRequest(`/api/board/${boardId}`, { method: "PUT", body: payload });
}

export function deleteBoard(boardId) {
  return apiRequest(`/api/board/${boardId}`, { method: "DELETE" });
}

export function likeBoard(boardId) {
  return apiRequest(`/api/board/${boardId}/likes`, { method: "POST" });
}

export function unlikeBoard(boardId) {
  return apiRequest(`/api/board/${boardId}/likes`, { method: "DELETE" });
}
