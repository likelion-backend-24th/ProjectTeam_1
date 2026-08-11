import { request } from './http'

// 회원가입: email, password, passwordConfirm, nickname, name
export function signup(payload) {
  return request('/auth/signup', { method: 'POST', body: payload, auth: false })
}

// 로그인: email, password -> { accessToken, refreshToken }
export function login(payload) {
  return request('/auth/login', { method: 'POST', body: payload, auth: false })
}

export function logout() {
  return request('/auth/logout', { method: 'POST' })
}
