import { apiRequest } from "./client";

export function getBoardComments(boardId) {
  return apiRequest(`/api/board/${boardId}/board-comments`);
}

export function createBoardComment(boardId, payload) {
  return apiRequest(`/api/board/${boardId}/board-comments`, { method: "POST", body: payload });
}

export function updateBoardComment(commentId, payload) {
  return apiRequest(`/api/board-comments/${commentId}`, { method: "PATCH", body: payload });
}

export function deleteBoardComment(commentId) {
  return apiRequest(`/api/board-comments/${commentId}`, { method: "DELETE" });
}
