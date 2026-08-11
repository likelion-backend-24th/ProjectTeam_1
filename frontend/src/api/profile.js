import { request, unwrap } from './http'

export async function getMyProfile() {
  const res = await request('/profile')
  return unwrap(res) // { name, nickName, email }
}
