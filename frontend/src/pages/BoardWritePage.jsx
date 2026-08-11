import React, { useState } from "react";
import api from "../api/axiosInstance";
import { useAuthStore } from "../stores/useAuthStore";

export function BoardWritePage({ onNavigate, editData = null }) {
  const isEdit = Boolean(editData);
  const { roleType } = useAuthStore();

  // DB의 role_type이 'ADMIN' 일 경우 공지사항 활성화
  const isAdmin = roleType === "ADMIN";

  const [category, setCategory] = useState(editData?.category || "FREE");
  const [title, setTitle] = useState(editData?.title || "");
  const [content, setContent] = useState(editData?.content || "");

  const categoryOptions = [
    { label: "자유게시판", value: "FREE" },
    { label: "질문", value: "QNA" },
    ...(isAdmin ? [{ label: "공지사항", value: "NOTICE" }] : []),
  ];

  const handleSubmit = async () => {
    if (!title.trim() || !content.trim()) {
      alert("제목과 내용을 모두 입력해 주세요.");
      return;
    }

    const payload = {
      title: title.trim(),
      content: content.trim(),
      category,
    };

    try {
      if (isEdit) {
        await api.put(`/api/board/${editData.id}`, payload);
        alert("게시글이 수정되었습니다.");
      } else {
        await api.post("/api/board", payload);
        alert("게시글이 등록되었습니다.");
      }
      onNavigate("boardList");
    } catch (err) {
      console.error("게시글 저장 실패:", err.response);
      const serverError =
        err.response?.data?.message || "서버 오류가 발생했습니다.";
      alert(`저장 실패 (${err.response?.status || 500}): ${serverError}`);
    }
  };

  return (
    <div className="flex flex-col h-full bg-white">
      <div className="px-4 py-3 flex justify-between items-center border-b border-gray-100">
        <button
          onClick={() => onNavigate("boardList")}
          className="p-1 text-gray-800"
        >
          <svg
            className="w-6 h-6"
            fill="none"
            stroke="currentColor"
            strokeWidth="2.5"
            viewBox="0 0 24 24"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              d="M6 18L18 6M6 6l12 12"
            />
          </svg>
        </button>
        <h1 className="font-bold text-base text-gray-900">
          {isEdit ? "게시글 수정" : "게시글 등록"}
        </h1>
        <button onClick={handleSubmit} className="p-1 text-gray-900">
          <svg
            className="w-6 h-6"
            fill="none"
            stroke="currentColor"
            strokeWidth="3"
            viewBox="0 0 24 24"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              d="M5 13l4 4L19 7"
            />
          </svg>
        </button>
      </div>

      <div className="p-4 space-y-4 flex-1">
        <div className="space-y-1.5">
          <label className="block text-xs font-bold text-gray-400">
            카테고리
          </label>
          <div className="flex space-x-2">
            {categoryOptions.map((opt) => (
              <button
                key={opt.value}
                type="button"
                onClick={() => setCategory(opt.value)}
                className={`px-4 py-2 rounded-full text-xs font-bold transition-colors ${
                  category === opt.value
                    ? "bg-blue-500 text-white"
                    : "bg-gray-100 text-gray-600"
                }`}
              >
                {opt.label}
              </button>
            ))}
          </div>
        </div>

        <div className="pt-2">
          <input
            type="text"
            placeholder="제목"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            className="w-full text-sm font-semibold py-2 border-b border-gray-100 focus:border-gray-400 focus:outline-none"
          />
        </div>

        <div className="pt-2">
          <textarea
            rows="14"
            placeholder="글 내용을 작성해주세요!"
            value={content}
            onChange={(e) => setContent(e.target.value)}
            className="w-full text-xs leading-relaxed focus:outline-none resize-none"
          ></textarea>
        </div>
      </div>
    </div>
  );
}
