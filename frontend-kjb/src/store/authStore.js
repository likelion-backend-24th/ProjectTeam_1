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

  // 👇 닉네임 등 프로필 수정을 위한 액션 추가
  updateProfile: async (payload) => {
    // 1. 서버에 프로필 수정 요청 (서버 구현에 맞게 함수명/파라미터 조정 필요)
    const updatedProfile = await updateProfileApi(payload);
    
    // 2. 스토어 내부의 profile 상태를 갱신 (서버 응답값 또는 기존 프로필과 병합)
    set((state) => ({
      profile: updatedProfile ?? { ...state.profile, ...payload },
    }));
  },
}));