import React, { useState } from "react";
import api from "../api/axiosInstance";

export function SignupPage({ onNavigate }) {
  const [form, setForm] = useState({
    email: "",
    password: "",
    passwordConfirm: "",
    nickname: "",
    name: "",
  });

  const handleChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (form.password !== form.passwordConfirm) {
      alert("비밀번호가 일치하지 않습니다.");
      return;
    }

    try {
      await api.post("/api/auth/signup", form);
      alert("회원가입이 완료되었습니다.");
      onNavigate("login");
    } catch (err) {
      alert(
        "회원가입 실패: " +
          (err.response?.data?.message || "입력값을 확인해 주세요."),
      );
    }
  };

  return (
    <div className="py-4">
      <h2 className="text-2xl font-bold text-center text-gray-800 mb-6">
        회원가입
      </h2>
      <form onSubmit={handleSubmit} className="space-y-3">
        <div>
          <label className="block text-xs font-bold text-gray-600 mb-1">
            이메일
          </label>
          <input
            name="email"
            type="email"
            required
            className="w-full p-3 border border-gray-200 rounded-xl text-sm focus:outline-none focus:border-blue-500"
            placeholder="email@example.com"
            onChange={handleChange}
          />
        </div>
        <div>
          <label className="block text-xs font-bold text-gray-600 mb-1">
            이름
          </label>
          <input
            name="name"
            type="text"
            required
            className="w-full p-3 border border-gray-200 rounded-xl text-sm focus:outline-none focus:border-blue-500"
            placeholder="홍길동"
            onChange={handleChange}
          />
        </div>
        <div>
          <label className="block text-xs font-bold text-gray-600 mb-1">
            닉네임
          </label>
          <input
            name="nickname"
            type="text"
            required
            className="w-full p-3 border border-gray-200 rounded-xl text-sm focus:outline-none focus:border-blue-500"
            placeholder="길동이"
            onChange={handleChange}
          />
        </div>
        <div>
          <label className="block text-xs font-bold text-gray-600 mb-1">
            비밀번호
          </label>
          <input
            name="password"
            type="password"
            required
            className="w-full p-3 border border-gray-200 rounded-xl text-sm focus:outline-none focus:border-blue-500"
            placeholder="비밀번호"
            onChange={handleChange}
          />
        </div>
        <div>
          <label className="block text-xs font-bold text-gray-600 mb-1">
            비밀번호 확인
          </label>
          <input
            name="passwordConfirm"
            type="password"
            required
            className="w-full p-3 border border-gray-200 rounded-xl text-sm focus:outline-none focus:border-blue-500"
            placeholder="비밀번호 재입력"
            onChange={handleChange}
          />
        </div>

        <button
          type="submit"
          className="w-full bg-green-600 text-white p-3.5 rounded-xl font-bold text-sm shadow-md hover:bg-green-700 transition-colors mt-4"
        >
          가입하기
        </button>
      </form>

      <div className="mt-4 text-center">
        <button
          onClick={() => onNavigate("login")}
          className="text-xs text-gray-500 underline hover:text-gray-800"
        >
          이미 계정이 있으신가요? 로그인
        </button>
      </div>
    </div>
  );
}
