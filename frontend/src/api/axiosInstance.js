import axios from "axios";
import { useAuthStore } from "../stores/useAuthStore";

const api = axios.create({
  baseURL: "http://54.86.192.52:8080", // 필요 시 백엔드 Base URL 설정 (예: 'http://localhost:8080')
  headers: {
    "Content-Type": "application/json",
  },
});

// 1. Request Interceptor: 모든 API 요청 헤더에 AccessToken 자동으로 싣기
api.interceptors.request.use(
  (config) => {
    const { accessToken } = useAuthStore.getState();
    if (accessToken) {
      config.headers.Authorization = `Bearer ${accessToken}`;
    }
    return config;
  },
  (error) => Promise.reject(error),
);

// 2. Response Interceptor: 401 에러(토큰 만료) 발생 시 /api/auth/reissue 자동 호출
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    // 401 에러이고, 이전에 재발급 시도를 한 적이 없는 요청인 경우
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;

      try {
        const { refreshToken } = useAuthStore.getState();

        // 토큰 재발급 API 호출
        // (RefreshToken을 헤더/바디 중 백엔드가 요구하는 방식으로 전달)
        const reissueRes = await axios.post(
          "/api/auth/reissue",
          { refreshToken },
          {
            headers: refreshToken
              ? { Authorization: `Bearer ${refreshToken}` }
              : {},
          },
        );

        const data = reissueRes.data.data || reissueRes.data;
        const newAccessToken = data.accessToken || data.token;
        const newRole = data.role_type || data.roleType || data.role;

        if (newAccessToken) {
          // Zustand 스토어 갱신
          useAuthStore.getState().updateToken(newAccessToken, newRole);

          // 실패했던 원래 요청에 새 토큰 세팅 후 재시도
          originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;
          return api(originalRequest);
        }
      } catch (reissueError) {
        // RefreshToken마저 만료된 경우 -> 강제 로그아웃
        console.error(
          "세션이 만료되었습니다. 다시 로그인해주세요.",
          reissueError,
        );
        useAuthStore.getState().logout();
        window.location.href = "/login";
        return Promise.reject(reissueError);
      }
    }

    return Promise.reject(error);
  },
);

export default api;
