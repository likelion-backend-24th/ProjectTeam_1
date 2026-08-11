import React, { useState } from "react";
import { useAuthStore } from "../stores/useAuthStore";
import api from "../api/axiosInstance";

export function LoginPage({ onNavigate }) {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");

  const { setAuthTokens } = useAuthStore();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErrorMessage("");

    if (!email || !password) {
      setErrorMessage("이메일과 비밀번호를 모두 입력해 주세요.");
      return;
    }

    try {
      setLoading(true);

      // POST /api/auth/login
      const res = await api.post("/api/auth/login", {
        email,
        password,
      });

      // 응답 데이터 구조 처리 (res.data.data 또는 res.data)
      const data = res.data.data || res.data;
      const accessToken = data.accessToken || data.token;
      const refreshToken = data.refreshToken;

      if (!accessToken) {
        throw new Error("응답에서 AccessToken을 찾을 수 없습니다.");
      }

      // Zustand 스토어에 토큰 저장
      // (내부에서 parseJwt 함수가 accessToken의 payload를 해독해 role: "ADMIN" / "USER"를 roleType에 자동으로 저장합니다)
      setAuthTokens(accessToken, refreshToken);

      alert("로그인되었습니다.");

      // 로그인 성공 후 메인/게시글 목록 페이지로 이동
      onNavigate("boardList");
    } catch (err) {
      console.error("로그인 오류:", err);
      if (err.response && err.response.data) {
        setErrorMessage(
          err.response.data.message ||
            "로그인에 실패했습니다. 아이디와 비밀번호를 확인해 주세요.",
        );
      } else {
        setErrorMessage(
          "서버와 통신할 수 없습니다. 잠시 후 다시 시도해 주세요.",
        );
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="p-4 max-w-sm mx-auto min-h-[80vh] flex flex-col justify-center">
      <div className="mb-8 text-center">
        <h1 className="text-2xl font-bold text-gray-900 mb-2">로그인</h1>
        <p className="text-xs text-gray-500">
          서비스 이용을 위해 계정 정보를 입력해 주세요.
        </p>
      </div>

      <form onSubmit={handleSubmit} className="space-y-4">
        {/* 이메일 입력 */}
        <div>
          <label className="block text-xs font-bold text-gray-600 mb-1.5">
            이메일 계정
          </label>
          <input
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="example@domain.com"
            required
            className="w-full p-3 bg-gray-50 border border-gray-200 rounded-xl text-xs font-medium text-gray-800 focus:outline-none focus:border-gray-400 transition-colors"
          />
        </div>

        {/* 비밀번호 입력 */}
        <div>
          <label className="block text-xs font-bold text-gray-600 mb-1.5">
            비밀번호
          </label>
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="비밀번호를 입력하세요"
            required
            className="w-full p-3 bg-gray-50 border border-gray-200 rounded-xl text-xs font-medium text-gray-800 focus:outline-none focus:border-gray-400 transition-colors"
          />
        </div>

        {/* 에러 메시지 표시 */}
        {errorMessage && (
          <div className="p-3 bg-red-50 border border-red-100 rounded-xl text-xs font-bold text-red-500 text-center">
            {errorMessage}
          </div>
        )}

        {/* 로그인 버튼 */}
        <button
          type="submit"
          disabled={loading}
          className="w-full py-3.5 mt-2 bg-gray-900 text-white font-bold text-xs rounded-xl hover:bg-black disabled:bg-gray-400 transition-colors shadow-sm"
        >
          {loading ? "로그인 중..." : "로그인"}
        </button>
      </form>

      {/* 하단 링크 */}
      <div className="mt-6 text-center text-xs text-gray-400 space-x-2">
        <span>계정이 없으신가요?</span>
        <button
          onClick={() => onNavigate("signup")}
          className="font-bold text-gray-800 hover:underline"
        >
          회원가입
        </button>
      </div>
    </div>
  );
}
