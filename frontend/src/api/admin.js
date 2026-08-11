import { request, toQueryString } from './http'

// 관리자 - ApiResponse 래핑 없이 원본 Page<UserResponseDto> 그대로 응답됨
export function getAllUsers({ page = 0, size = 10 } = {}) {
  const qs = toQueryString({ page, size, sort: 'id,desc' })
  return request(`/admin/users${qs}`)
}

export function patchUser(userId, { status, roleType }) {
  return request(`/admin/users/${userId}`, { method: 'PATCH', body: { status, roleType } })
}
