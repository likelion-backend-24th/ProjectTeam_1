const BASE_URL = '/api'
const TOKEN_KEY = 'cf_access_token'
const REFRESH_KEY = 'cf_refresh_token'

export class ApiError extends Error {
  constructor(message, status, code) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.code = code
  }
}

export function getAccessToken() {
  return localStorage.getItem(TOKEN_KEY)
}

export function getRefreshToken() {
  return localStorage.getItem(REFRESH_KEY)
}

export function setTokens({ accessToken, refreshToken }) {
  if (accessToken) localStorage.setItem(TOKEN_KEY, accessToken)
  if (refreshToken) localStorage.setItem(REFRESH_KEY, refreshToken)
}

export function clearTokens() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(REFRESH_KEY)
}

async function parseBody(res) {
  const text = await res.text()
  if (!text) return null
  const contentType = res.headers.get('content-type') || ''
  if (contentType.includes('application/json')) {
    try {
      return JSON.parse(text)
    } catch {
      return text
    }
  }
  return text
}

// accessToken 만료(401) 시 refreshToken으로 재발급받는다. 동시에 여러 요청이 401을 맞아도
// 재발급은 한 번만 실행되도록 진행 중인 Promise를 공유한다.
let refreshPromise = null

function refreshAccessToken() {
  if (!refreshPromise) {
    refreshPromise = (async () => {
      const refreshToken = getRefreshToken()
      if (!refreshToken) throw new ApiError('로그인이 필요합니다.', 401)

      const res = await fetch(`${BASE_URL}/auth/reissue`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ refreshToken }),
      })
      const parsed = await parseBody(res)

      if (!res.ok) {
        clearTokens()
        const message = (parsed && typeof parsed === 'object' && parsed.message) || '세션이 만료되었습니다. 다시 로그인해주세요.'
        throw new ApiError(message, res.status)
      }

      setTokens(parsed)
      return parsed.accessToken
    })().finally(() => {
      refreshPromise = null
    })
  }
  return refreshPromise
}

// core fetch wrapper: attaches JWT, parses JSON/text body, throws ApiError on failure
export async function request(path, { method = 'GET', body, auth = true, headers = {} } = {}, isRetry = false) {
  const finalHeaders = { ...headers }
  if (body !== undefined) finalHeaders['Content-Type'] = 'application/json'
  if (auth) {
    const token = getAccessToken()
    if (token) finalHeaders['Authorization'] = `Bearer ${token}`
  }

  const res = await fetch(`${BASE_URL}${path}`, {
    method,
    headers: finalHeaders,
    body: body !== undefined ? JSON.stringify(body) : undefined,
  })

  const parsed = await parseBody(res)

  if (!res.ok) {
    // accessToken 만료로 인한 401이면 재발급 후 원래 요청을 한 번만 재시도한다.
    if (res.status === 401 && auth && !isRetry && path !== '/auth/reissue' && getRefreshToken()) {
      try {
        await refreshAccessToken()
        return request(path, { method, body, auth, headers }, true)
      } catch {
        // 재발급도 실패하면 아래에서 원래 401 에러를 그대로 던진다.
      }
    }

    const message =
      (parsed && typeof parsed === 'object' && parsed.message) ||
      (typeof parsed === 'string' ? parsed : null) ||
      res.statusText ||
      '요청 처리 중 오류가 발생했습니다.'
    const code = parsed && typeof parsed === 'object' ? parsed.code : undefined
    throw new ApiError(message, res.status, code)
  }

  return parsed
}

// unwraps the backend's ApiResponse<T> envelope ({ success, code, message, data }) when present
export function unwrap(parsed) {
  if (parsed && typeof parsed === 'object' && 'success' in parsed && 'data' in parsed) {
    return parsed.data
  }
  return parsed
}

export function toQueryString(params) {
  const search = new URLSearchParams()
  Object.entries(params || {}).forEach(([key, value]) => {
    if (value === undefined || value === null || value === '') return
    search.set(key, value)
  })
  const qs = search.toString()
  return qs ? `?${qs}` : ''
}
