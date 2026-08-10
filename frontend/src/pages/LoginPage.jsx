import React, { useState } from "react";
import api from "../api/axiosInstance";
import { useAuthStore } from "../stores/useAuthStore";

export function LoginPage({ onNavigate }) {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const setAuth = useAuthStore((state) => state.setAuth);

  const handleLogin = async (e) => {
    e.preventDefault();
    try {
      console.log("로그인 요청 데이터:", { email, password });

      const res = await api.post("/api/auth/login", { email, password });
      console.log("로그인 응답 본문(res.data):", res.data);

      const data = res.data.data || res.data;

      // 토큰 존재 여부 확인
      if (!data.accessToken && !data.token) {
        console.error("응답에 accessToken이 없습니다!", data);
        alert("로그인 응답에 토큰이 없습니다. 백엔드 DTO를 확인하세요.");
        return;
      }

      // Zustand 스토어 및 localStorage에 저장
      setAuth(data);

      alert("로그인되었습니다.");
      onNavigate("boardList");
    } catch (err) {
      console.error("로그인 에러 상세:", err.response);
      const statusCode = err.response?.status;
      const errorMessage =
        err.response?.data?.message || err.response?.data || "서버 응답 오류";

      alert(
        `로그인 실패 (${statusCode || "네트워크 에러"}): ${typeof errorMessage === "string" ? errorMessage : "이메일 또는 비밀번호를 확인하세요."}`,
      );
    }
  };

  return (
    <div className="max-w-md mx-auto my-10 p-6 bg-white rounded-xl border border-gray-100 shadow-sm space-y-4">
      <h2 className="text-xl font-bold text-center text-gray-800">로그인</h2>
      <form onSubmit={handleLogin} className="space-y-3">
        <div>
          <label className="block text-xs font-bold text-gray-500 mb-1">
            이메일
          </label>
          <input
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            className="w-full p-2.5 border rounded-lg text-sm focus:outline-none focus:border-blue-500"
            required
          />
        </div>
        <div>
          <label className="block text-xs font-bold text-gray-500 mb-1">
            비밀번호
          </label>
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            className="w-full p-2.5 border rounded-lg text-sm focus:outline-none focus:border-blue-500"
            required
          />
        </div>
        <button
          type="submit"
          className="w-full bg-blue-600 text-white py-2.5 rounded-lg text-xs font-bold hover:bg-blue-700 transition-colors"
        >
          로그인
        </button>
      </form>
    </div>
  );
}
