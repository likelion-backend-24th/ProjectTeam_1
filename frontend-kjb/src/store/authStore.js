import { create } from "zustand";
import { clearAccessToken, getAccessToken, setAccessToken } from "@/lib/api/client";
import { getMyProfile } from "@/lib/api/profile";
import { login as loginApi, logout as logoutApi } from "@/lib/api/auth";
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

  logout: async () => {
    try {
      await logoutApi();
    } catch {
      // ignore network/server errors on logout, clear local state regardless
    }
    clearAccessToken();
    set({ isAuthenticated: false, profile: null, isAdmin: false });
  },
}));
