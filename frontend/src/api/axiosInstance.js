import axios from "axios";

const api = axios.create({
  baseURL: "http://localhost:8080",
});

// 요청 인터셉터: 로컬 스토리지에 토큰이 있을 때만 Authorization 헤더 첨부
api.interceptors.request.use(
  (config) => {
    try {
      const persisted = localStorage.getItem("auth-storage");
      if (persisted) {
        const parsed = JSON.parse(persisted);
        const token = parsed?.state?.accessToken;
        if (token) {
          config.headers.Authorization = `Bearer ${token}`;
        }
      }
    } catch (e) {
      console.error("토큰 파싱 실패:", e);
    }
    return config;
  },
  (error) => Promise.reject(error),
);

// 응답 인터셉터: 401 에러 처리 보완
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    if (
      error.response?.status === 401 &&
      !originalRequest._retry &&
      !originalRequest.url.includes("/reissue")
    ) {
      originalRequest._retry = true;

      // 1. 로컬스토리지에서 refreshToken 확인
      const persisted = localStorage.getItem("auth-storage");
      let refreshToken = null;

      if (persisted) {
        try {
          const parsed = JSON.parse(persisted);
          refreshToken = parsed?.state?.refreshToken;
        } catch (e) {
          console.error(e);
        }
      }

      // 2. 비로그인 유저(refreshToken 없음)인 경우: 튕겨내지 않고 그대로 에러 반환
      if (!refreshToken) {
        return Promise.reject(error);
      }

      // 3. 로그인 유저였으나 AccessToken이 만료된 경우: /reissue 실행
      try {
        const res = await axios.post("http://localhost:8080/reissue", {
          refreshToken: refreshToken,
        });

        const { accessToken: newAccessToken, refreshToken: newRefreshToken } =
          res.data;

        if (newAccessToken) {
          const parsed = JSON.parse(persisted);
          parsed.state.accessToken = newAccessToken;
          if (newRefreshToken) {
            parsed.state.refreshToken = newRefreshToken;
          }
          localStorage.setItem("auth-storage", JSON.stringify(parsed));

          originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;
          return api(originalRequest);
        }
      } catch (reissueError) {
        // RefreshToken마저 만료된 경우만 로그아웃 및 리다이렉트
        console.error("세션 만료:", reissueError);
        localStorage.removeItem("auth-storage");
        window.location.href = "/";
        return Promise.reject(reissueError);
      }
    }

    return Promise.reject(error);
  },
);

export default api;
