import { request, unwrap } from './http'

export async function likeBoard(boardId) {
  const res = await request(`/board/${boardId}/likes`, { method: 'POST' })
  return unwrap(res) // { liked, likeCount }
}

export async function unlikeBoard(boardId) {
  const res = await request(`/board/${boardId}/likes`, { method: 'DELETE' })
  return unwrap(res)
}
