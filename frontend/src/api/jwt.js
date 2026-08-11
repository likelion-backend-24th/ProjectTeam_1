// accessToken 내 payload(userId, email, role)를 클라이언트에서 읽기 위한 디코더.
// 서명 검증은 하지 않음 - 실제 인가는 항상 백엔드가 수행함.
export function decodeJwt(token) {
  try {
    const payload = token.split('.')[1]
    const base64 = payload.replace(/-/g, '+').replace(/_/g, '/')
    const json = decodeURIComponent(
      atob(base64)
        .split('')
        .map((c) => '%' + c.charCodeAt(0).toString(16).padStart(2, '0'))
        .join(''),
    )
    const claims = JSON.parse(json)
    return {
      userId: Number(claims.sub),
      email: claims.email,
      role: claims.role,
      exp: claims.exp,
    }
  } catch {
    return null
  }
}

export function isExpired(claims) {
  if (!claims?.exp) return true
  return Date.now() >= claims.exp * 1000
}
