import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import * as authApi from '../api/auth'
import { getMyProfile } from '../api/profile'
import { getAccessToken, setTokens, clearTokens } from '../api/http'
import { decodeJwt } from '../api/jwt'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null) // { userId, email, role, name, nickName }
  const [initializing, setInitializing] = useState(true)

  const loadProfile = useCallback(async (claims) => {
    try {
      const profile = await getMyProfile()
      setUser({
        userId: claims.userId,
        email: claims.email,
        role: claims.role,
        name: profile.name,
        nickName: profile.nickName,
      })
    } catch {
      // 토큰이 유효하지 않거나 만료된 경우
      clearTokens()
      setUser(null)
    }
  }, [])

  useEffect(() => {
    const token = getAccessToken()
    if (!token) {
      setInitializing(false)
      return
    }
    const claims = decodeJwt(token)
    if (!claims) {
      clearTokens()
      setInitializing(false)
      return
    }
    // accessToken이 만료됐어도 loadProfile -> getMyProfile 요청이 401을 맞으면
    // http.js가 refreshToken으로 자동 재발급 후 재시도한다.
    loadProfile(claims).finally(() => setInitializing(false))
  }, [loadProfile])

  const login = useCallback(
    async (email, password) => {
      const result = await authApi.login({ email, password })
      setTokens(result)
      const claims = decodeJwt(result.accessToken)
      if (claims) {
        await loadProfile(claims)
      }
      return result
    },
    [loadProfile],
  )

  const logout = useCallback(async () => {
    try {
      await authApi.logout()
    } catch {
      // 서버 로그아웃 실패해도 클라이언트 세션은 정리한다
    }
    clearTokens()
    setUser(null)
  }, [])

  const value = useMemo(
    () => ({
      user,
      initializing,
      isAuthenticated: !!user,
      isAdmin: user?.role === 'ADMIN',
      login,
      logout,
    }),
    [user, initializing, login, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth는 AuthProvider 내부에서만 사용할 수 있습니다.')
  return ctx
}
