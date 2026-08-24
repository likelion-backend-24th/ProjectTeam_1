import { create } from "zustand";
import { clearAccessToken, getAccessToken, setAccessToken } from "@/lib/api/client";
import { getMyProfile, updateProfile, checkPassword, updatePassword } from "@/lib/api/profile"; // 👈 checkPassword, updatePassword 추가
import { login as loginApi, logout as logoutApi, withdraw as withdrawApi } from "@/lib/api/auth";
import { decodeJwtPayload } from "@/lib/jwt";

function isAdminToken(token) {
  const payload = decodeJwtPayload(token);
  return payload?.role === "ADMIN";
}

function isHostToken(token) {
  const payload = decodeJwtPayload(token);
  return payload?.role === "HOST";
}

export const useAuthStore = create((set, get) => ({
  isAuthenticated: false,
  profile: null,
  isAdmin: false,
  isHost: false,
  isLoading: true,
  hasInitialized: false,

  init: async () => {
    if (get().hasInitialized) return;
    set({ hasInitialized: true });

    const token = getAccessToken();
    if (!token) {
      set({ isAuthenticated: false, profile: null, isAdmin: false, isHost: false, isLoading: false });
      return;
    }
    try {
      const profile = await getMyProfile();
      set({ profile, isAuthenticated: true, isAdmin: isAdminToken(token), isHost: isHostToken(token), isLoading: false });
    } catch {
      clearAccessToken();
      set({ isAuthenticated: false, profile: null, isAdmin: false, isHost: false, isLoading: false });
    }
  },

  login: async (payload) => {
    const res = await loginApi(payload);
    setAccessToken(res.accessToken);
    const profile = await getMyProfile();
    set({ profile, isAuthenticated: true, isAdmin: isAdminToken(res.accessToken), isHost: isHostToken(res.accessToken) });
  },

  socialLogin: async (accessToken) => {
    setAccessToken(accessToken);
    try {
      const profile = await getMyProfile();
      set({ profile, isAuthenticated: true, isAdmin: isAdminToken(accessToken), isHost: isHostToken(accessToken), isLoading: false });
    } catch {
      clearAccessToken();
      set({ isAuthenticated: false, profile: null, isAdmin: false, isHost: false, isLoading: false });
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
    set({ isAuthenticated: false, profile: null, isAdmin: false, isHost: false });
  },

  withdraw: async () => {
    await withdrawApi();
    clearAccessToken();
    set({ isAuthenticated: false, profile: null, isAdmin: false, isHost: false });
  },

  updateProfile: async (payload) => {
    const updatedProfile = await updateProfile(payload);
    
    set((state) => ({
      profile: updatedProfile ?? { ...state.profile, ...payload },
    }));
  },

  // 현재 비밀번호 확인 액션
  checkPassword: async (payload) => {
    return await checkPassword(payload);
  },

  // 비밀번호 변경 액션
  changePassword: async (payload) => {
    await updatePassword(payload);
  },
}));