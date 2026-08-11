import { request } from './http'

// 답글(QNA 답변) - ApiResponse 래핑 없이 원본 DTO 그대로 응답됨
export function getReplies(boardId) {
  return request(`/board/${boardId}/reply`, { auth: false })
}

export function createReply(boardId, content) {
  return request(`/board/${boardId}/reply`, { method: 'POST', body: { content } })
}

export function updateReply(replyId, content) {
  return request(`/reply/${replyId}`, { method: 'PATCH', body: { content } })
}

export function deleteReply(replyId) {
  return request(`/reply/${replyId}`, { method: 'DELETE' })
}
