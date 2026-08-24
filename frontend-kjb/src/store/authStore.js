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
      clearRefreshToken();
      set({ isAuthenticated: false, profile: null, isAdmin: false, isHost: false, isLoading: false });
    }
  },

  login: async (payload) => {
    const res = await loginApi(payload);
    setAccessToken(res.accessToken);
    if (res.refreshToken) setRefreshToken(res.refreshToken);
    const profile = await getMyProfile();
    set({
      profile,
      isAuthenticated: true,
      isAdmin: isAdminToken(res.accessToken),
      isHost: isHostToken(res.accessToken),
    });
  },

  socialLogin: async (accessToken) => {
    setAccessToken(accessToken);
    try {
      const profile = await getMyProfile();
      set({
        profile,
        isAuthenticated: true,
        isAdmin: isAdminToken(accessToken),
        isHost: isHostToken(accessToken),
        isLoading: false,
      });
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

  promoteToHost: async (payload) => {
    await promoteToHostApi(payload);

    const refreshToken = getRefreshToken();
    if (refreshToken) {
      const res = await reissueApi(refreshToken);
      setAccessToken(res.accessToken);
      if (res.refreshToken) setRefreshToken(res.refreshToken);
      set({ isAdmin: isAdminToken(res.accessToken), isHost: isHostToken(res.accessToken) });
    } else {
      set({ isHost: true });
    }
  },
}));