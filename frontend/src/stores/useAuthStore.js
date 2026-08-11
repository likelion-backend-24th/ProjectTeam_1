import { create } from "zustand";
import { persist, createJSONStorage } from "zustand/middleware";
import { parseJwt } from "../utils/jwt";

export const useAuthStore = create(
  persist(
    (set, get) => ({
      accessToken: null,
      refreshToken: null,
      email: null, // GET /api/profile 응답값
      nickName: null, // GET /api/profile 응답값 (활동명)
      name: null, // GET /api/profile 응답값 (성명)
      roleType: "USER", // JWT "role" 기반 ('ADMIN' 또는 'USER')

      /**
       * 1. 로그인 / 토큰 발급 시 호출
       * JWT Payload에서 role("ADMIN" | "USER") 및 email 추출
       */
      setAuthTokens: (accessToken, refreshToken) => {
        const payload = parseJwt(accessToken);

        // JWT의 "role" 필드 검사 (ROLE_ADMIN, ADMIN 등 모두 대응)
        const rawRole = payload?.role || "USER";
        const normalizedRole = String(rawRole).toUpperCase().includes("ADMIN")
          ? "ADMIN"
          : "USER";

        set({
          accessToken,
          refreshToken: refreshToken ?? get().refreshToken,
          email: payload?.email ?? get().email,
          roleType: normalizedRole,
        });
      },

      /**
       * 2. GET /api/profile 성공 시 인적사항 업데이트
       * dto: { name, nickName, email }
       */
      setProfile: ({ name, nickName, email }) => {
        set({
          name,
          nickName,
          email: email ?? get().email,
        });
      },

      /**
       * 3. /api/auth/reissue 토큰 재발급 성공 시
       */
      updateAccessToken: (newAccessToken) => {
        const payload = parseJwt(newAccessToken);
        const rawRole = payload?.role;

        set((state) => ({
          accessToken: newAccessToken,
          roleType: rawRole
            ? String(rawRole).toUpperCase().includes("ADMIN")
              ? "ADMIN"
              : "USER"
            : state.roleType,
        }));
      },

      /**
       * 4. 로그아웃
       */
      logout: () =>
        set({
          accessToken: null,
          refreshToken: null,
          email: null,
          nickName: null,
          name: null,
          roleType: "USER",
        }),
    }),
    {
      name: "auth-storage",
      storage: createJSONStorage(() => localStorage),
    },
  ),
);
