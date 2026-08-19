import { apiRequest } from "./client";

export function getReplies(boardId) {
  return apiRequest(`/api/board/${boardId}/reply`);
}

export function createReply(boardId, payload) {
  return apiRequest(`/api/board/${boardId}/reply`, { method: "POST", body: payload });
}

export function updateReply(replyId, payload) {
  return apiRequest(`/api/reply/${replyId}`, { method: "PATCH", body: payload });
}

export function deleteReply(replyId) {
  return apiRequest(`/api/reply/${replyId}`, { method: "DELETE" });
}

export function getReplyComments(replyId) {
  return apiRequest(`/api/reply/${replyId}/reply-comments`);
}

export function createReplyComment(replyId, payload) {
  return apiRequest(`/api/reply/${replyId}/reply-comments`, { method: "POST", body: payload });
}

export function deleteReplyComment(commentId) {
  return apiRequest(`/api/reply-comments/${commentId}`, { method: "DELETE" });
}
