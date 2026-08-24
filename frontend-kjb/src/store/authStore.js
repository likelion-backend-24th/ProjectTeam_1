import { create } from "zustand";
import {
  clearAccessToken,
  getAccessToken,
  setAccessToken,
  getRefreshToken,
  setRefreshToken,
  clearRefreshToken,
} from "@/lib/api/client";
import {
  getMyProfile,
  updateProfile as updateProfileApi,
  checkPassword as checkPasswordApi,
  updatePassword as updatePasswordApi,
  promoteToHost as promoteToHostApi,
} from "@/lib/api/profile";
import { login as loginApi, logout as logoutApi, withdraw as withdrawApi, reissue as reissueApi } from "@/lib/api/auth";
import { decodeJwtPayload } from "@/lib/jwt";

function getRoleFromToken(token) {
  const payload = decodeJwtPayload(token);
  return payload?.role ?? null;
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
      const role = getRoleFromToken(token);
      set({ profile, isAuthenticated: true, isAdmin: role === "ADMIN", isHost: role === "HOST", isLoading: false });
    } catch {
      clearAccessToken();
      clearRefreshToken();
      set({ isAuthenticated: false, profile: null, isAdmin: false, isHost: false, isLoading: false });
    }
  },

  login: async (payload) => {
    const res = await loginApi(payload);
    setAccessToken(res.accessToken);
    if (res.refreshToken) setRefreshToken(res.refreshToken);
    const profile = await getMyProfile();
    const role = getRoleFromToken(res.accessToken);
    set({ profile, isAuthenticated: true, isAdmin: role === "ADMIN", isHost: role === "HOST" });
  },

  socialLogin: async (accessToken) => {
    setAccessToken(accessToken);
    try {
      const profile = await getMyProfile();
      const role = getRoleFromToken(accessToken);
      set({ profile, isAuthenticated: true, isAdmin: role === "ADMIN", isHost: role === "HOST", isLoading: false });
    } catch {
      clearAccessToken();
      clearRefreshToken();
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
    clearRefreshToken();
    set({ isAuthenticated: false, profile: null, isAdmin: false, isHost: false });
  },

  withdraw: async () => {
    await withdrawApi();
    clearAccessToken();
    clearRefreshToken();
    set({ isAuthenticated: false, profile: null, isAdmin: false, isHost: false });
  },

  updateProfile: async (payload) => {
    const updatedProfile = await updateProfileApi(payload);
    set((state) => ({
      profile: updatedProfile ?? { ...state.profile, ...payload },
    }));
  },

  checkPassword: async (payload) => {
    return await checkPasswordApi(payload);
  },

  changePassword: async (payload) => {
    await updatePasswordApi(payload);
  },

  // 호스트 승격 신청 후, 리프레시 토큰으로 새 accessToken을 받아와서 role을 즉시 갱신
  promoteToHost: async (payload) => {
    await promoteToHostApi(payload);

    const refreshToken = getRefreshToken();
    if (refreshToken) {
      const res = await reissueApi(refreshToken);
      setAccessToken(res.accessToken);
      if (res.refreshToken) setRefreshToken(res.refreshToken);
      const role = getRoleFromToken(res.accessToken);
      set({ isAdmin: role === "ADMIN", isHost: role === "HOST" });
    } else {
      // 리프레시 토큰이 없는 경우(소셜 로그인 등) 즉시 반영이 안 될 수 있어요
      set({ isHost: true });
    }
  },
}));