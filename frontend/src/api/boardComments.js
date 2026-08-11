import { request, unwrap } from './http'

export async function getBoardComments(boardId) {
  const res = await request(`/board/${boardId}/board-comments`, { auth: false })
  return unwrap(res)
}

export async function createBoardComment(boardId, content) {
  const res = await request(`/board/${boardId}/board-comments`, { method: 'POST', body: { content } })
  return unwrap(res)
}

export async function updateBoardComment(commentId, content) {
  const res = await request(`/board-comments/${commentId}`, { method: 'PATCH', body: { content } })
  return unwrap(res)
}

export function deleteBoardComment(commentId) {
  return request(`/board-comments/${commentId}`, { method: 'DELETE' })
}
