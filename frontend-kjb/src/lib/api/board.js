import { apiRequest } from "./client";

export function getBoards({ type, keyword, page = 0, size = 10, sort } = {}) {
  return apiRequest("/api/board", { query: { type, keyword, page, size, sort } });
}

export function getBoard(boardId, signal) {
  return apiRequest(`/api/board/${boardId}`, { signal });
}

function toBoardFormData({ title, content, category }, images) {
  const formData = new FormData();
  formData.append(
    "request",
    new Blob([JSON.stringify({ title, content, category })], { type: "application/json" }),
  );
  (images ?? []).forEach((image) => formData.append("images", image));
  return formData;
}

export function createBoard(payload, images) {
  return apiRequest("/api/board", { method: "POST", body: toBoardFormData(payload, images) });
}

export function updateBoard(boardId, payload, images) {
  return apiRequest(`/api/board/${boardId}`, { method: "PUT", body: toBoardFormData(payload, images) });
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
