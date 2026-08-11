import { request, unwrap, toQueryString } from './http'

// type: 'title' | 'content' | 'author' (검색 필드). keyword 없으면 전체 조회.
// category: 'FREE' | 'QNA' | 'NOTICE' (없으면 전체 카테고리)
export async function getBoards({ type, keyword, category, page = 0, size = 10, sort = 'createdAt,desc' } = {}) {
  const qs = toQueryString({ type, keyword, category, page, size, sort })
  const res = await request(`/board${qs}`)
  return unwrap(res) // Page<BoardResponseDto>
}

export async function getBoard(boardId) {
  const res = await request(`/board/${boardId}`)
  return unwrap(res)
}

export async function createBoard({ title, content, category }) {
  const res = await request('/board', { method: 'POST', body: { title, content, category } })
  return unwrap(res)
}

export async function updateBoard(boardId, { title, content, category }) {
  const res = await request(`/board/${boardId}`, { method: 'PUT', body: { title, content, category } })
  return unwrap(res)
}

export function deleteBoard(boardId) {
  return request(`/board/${boardId}`, { method: 'DELETE' })
}
