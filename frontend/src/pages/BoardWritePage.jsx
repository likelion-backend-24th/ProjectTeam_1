import React, { useState } from "react";
import api from "../api/axiosInstance";
import { useAuthStore } from "../stores/useAuthStore";

export function BoardWritePage({ onNavigate }) {
  const { roleType } = useAuthStore();
  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");
  const [category, setCategory] = useState("FREE");

  // ADMIN 권한 유연하게 검증 (ROLE_ADMIN 또는 ADMIN)
  const isAdmin = roleType === "ADMIN" || roleType === "ROLE_ADMIN";

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      await api.post("/api/board", { title, content, category });
      alert("게시글이 등록되었습니다.");
      onNavigate("boardList");
    } catch (err) {
      alert(
        "게시글 등록 실패: " +
          (err.response?.data?.message || "오류가 발생했습니다."),
      );
    }
  };

  return (
    <div className="py-2 space-y-4">
      <h2 className="text-xl font-bold text-gray-800">게시글 작성</h2>
      <form
        onSubmit={handleSubmit}
        className="bg-white p-4 rounded-xl border border-gray-100 shadow-sm space-y-3"
      >
        <div>
          <label className="block text-xs font-bold text-gray-500 mb-1">
            카테고리
          </label>
          <select
            value={category}
            onChange={(e) => setCategory(e.target.value)}
            className="w-full p-2.5 border rounded-lg text-sm bg-white"
          >
            <option value="FREE">자유게시판 (FREE)</option>
            <option value="QNA">질문답변 (QNA)</option>
            {isAdmin && <option value="NOTICE">공지사항 (NOTICE)</option>}
          </select>
        </div>
        <div>
          <label className="block text-xs font-bold text-gray-500 mb-1">
            제목
          </label>
          <input
            type="text"
            placeholder="제목을 입력하세요"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            className="w-full p-2.5 border rounded-lg text-sm focus:outline-none focus:border-blue-500"
            required
          />
        </div>
        <div>
          <label className="block text-xs font-bold text-gray-500 mb-1">
            내용
          </label>
          <textarea
            rows={8}
            placeholder="내용을 입력하세요"
            value={content}
            onChange={(e) => setContent(e.target.value)}
            className="w-full p-2.5 border rounded-lg text-sm focus:outline-none focus:border-blue-500 resize-none"
            required
          />
        </div>
        <div className="flex gap-2 pt-2">
          <button
            type="button"
            onClick={() => onNavigate("boardList")}
            className="flex-1 bg-gray-100 py-2.5 rounded-lg text-xs font-bold text-gray-600 hover:bg-gray-200"
          >
            취소
          </button>
          <button
            type="submit"
            className="flex-1 bg-blue-600 text-white py-2.5 rounded-lg text-xs font-bold hover:bg-blue-700"
          >
            등록하기
          </button>
        </div>
      </form>
    </div>
  );
}
