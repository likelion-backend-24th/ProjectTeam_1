import { request, unwrap } from './http'

export async function getReplyComments(replyId) {
  const res = await request(`/reply/${replyId}/reply-comments`)
  return unwrap(res)
}

export async function createReplyComment(replyId, content) {
  const res = await request(`/reply/${replyId}/reply-comments`, { method: 'POST', body: { content } })
  return unwrap(res)
}

export async function updateReplyComment(commentId, content) {
  const res = await request(`/reply-comments/${commentId}`, { method: 'PATCH', body: { content } })
  return unwrap(res)
}

export function deleteReplyComment(commentId) {
  return request(`/reply-comments/${commentId}`, { method: 'DELETE' })
}
