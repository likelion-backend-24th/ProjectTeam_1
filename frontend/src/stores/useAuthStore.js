import { create } from "zustand";
import { persist } from "zustand/middleware";

export const useAuthStore = create(
  persist(
    (set) => ({
      accessToken: null,
      refreshToken: null, // refreshToken 추가
      userId: null,
      roleType: null,
      nickname: null,

      setAuth: (data) =>
        set({
          accessToken: data.accessToken || data.token,
          refreshToken: data.refreshToken, // refreshToken 추가
          userId: data.userId || data.id,
          roleType: data.roleType || data.role,
          nickname: data.nickname || data.nickName,
        }),

      setAccessToken: (newToken) => set({ accessToken: newToken }),

      logout: () =>
        set({
          accessToken: null,
          refreshToken: null,
          userId: null,
          roleType: null,
          nickname: null,
        }),
    }),
    {
      name: "auth-storage",
    },
  ),
);
