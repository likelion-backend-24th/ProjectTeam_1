import { create } from "zustand";
import { clearAccessToken, getAccessToken, setAccessToken } from "@/lib/api/client";
import { getMyProfile } from "@/lib/api/profile";
import { login as loginApi, logout as logoutApi, withdraw as withdrawApi } from "@/lib/api/auth";
import { decodeJwtPayload } from "@/lib/jwt";

function isAdminToken(token) {
  const payload = decodeJwtPayload(token);
  return payload?.role === "ADMIN";
}

export const useAuthStore = create((set, get) => ({
  isAuthenticated: false,
  profile: null,
  isAdmin: false,
  isLoading: true,
  hasInitialized: false,

  init: async () => {
    if (get().hasInitialized) return;
    set({ hasInitialized: true });

    const token = getAccessToken();
    if (!token) {
      set({ isAuthenticated: false, profile: null, isAdmin: false, isLoading: false });
      return;
    }
    try {
      const profile = await getMyProfile();
      set({ profile, isAuthenticated: true, isAdmin: isAdminToken(token), isLoading: false });
    } catch {
      clearAccessToken();
      set({ isAuthenticated: false, profile: null, isAdmin: false, isLoading: false });
    }
  },

  login: async (payload) => {
    const res = await loginApi(payload);
    setAccessToken(res.accessToken);
    const profile = await getMyProfile();
    set({ profile, isAuthenticated: true, isAdmin: isAdminToken(res.accessToken) });
  },

  socialLogin: async (accessToken) => {
    setAccessToken(accessToken);
    try {
      const profile = await getMyProfile();
      set({ profile, isAuthenticated: true, isAdmin: isAdminToken(accessToken), isLoading: false });
    } catch {
      clearAccessToken();
      set({ isAuthenticated: false, profile: null, isAdmin: false, isLoading: false });
      throw new Error("소셜 로그인 프로필을 불러오지 못했습니다.");
    }
  },

  logout: async () => {
    try {
      await logoutApi();
    } catch {
      // ignore network/server errors on logout, clear local state regardless
    }
    clearAccessToken();
    set({ isAuthenticated: false, profile: null, isAdmin: false });
  },

  withdraw: async () => {
    await withdrawApi();
    clearAccessToken();
    set({ isAuthenticated: false, profile: null, isAdmin: false });
  },
}));