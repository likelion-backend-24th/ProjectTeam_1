import React, { useEffect, useState } from "react";
import api from "../api/axiosInstance";
import { useAuthStore } from "../stores/useAuthStore";

const CATEGORIES = ["ALL", "FREE", "QNA", "NOTICE"];

export function BoardListPage({ onSelectBoard, onNavigate }) {
  const [category, setCategory] = useState("ALL");
  const [page, setPage] = useState(0);
  const [boardPage, setBoardPage] = useState({ content: [], totalPages: 0 });

  const accessToken = useAuthStore((state) => state.accessToken);

  useEffect(() => {
    fetchBoards();
  }, [category, page]);

  const fetchBoards = async () => {
    try {
      const params = { page, size: 10 };
      if (category !== "ALL") params.category = category;

      const res = await api.get("/api/board", { params });
      const pageData = res.data.data;
      setBoardPage({
        content: pageData?.content || [],
        totalPages: pageData?.totalPages || 0,
      });
    } catch (err) {
      console.error("게시글 목록 로드 실패", err);
    }
  };

  return (
    <div className="py-2">
      {/* 카테고리 필터 & 글쓰기 버튼 */}
      <div className="flex justify-between items-center mb-4 border-b border-gray-100 pb-2">
        <div className="flex gap-1.5 overflow-x-auto">
          {CATEGORIES.map((cat) => (
            <button
              key={cat}
              onClick={() => {
                setCategory(cat);
                setPage(0);
              }}
              className={`px-3 py-1.5 rounded-full text-xs font-semibold whitespace-nowrap transition-colors ${
                category === cat
                  ? "bg-blue-600 text-white"
                  : "bg-gray-100 text-gray-600 hover:bg-gray-200"
              }`}
            >
              {cat}
            </button>
          ))}
        </div>

        {/* 로그인한 유저만 글쓰기 버튼 표시 */}
        {accessToken && (
          <button
            onClick={() => onNavigate("boardWrite")}
            className="bg-blue-600 text-white text-xs font-bold px-3 py-1.5 rounded-lg shadow-sm hover:bg-blue-700 whitespace-nowrap ml-2"
          >
            + 글쓰기
          </button>
        )}
      </div>

      {/* 게시글 리스트 */}
      <div className="space-y-3">
        {boardPage.content.map((board) => (
          <div
            key={board.id}
            onClick={() => onSelectBoard(board.id)}
            className="p-4 border border-gray-100 rounded-xl shadow-sm hover:border-blue-400 bg-white transition-all cursor-pointer"
          >
            <div className="flex justify-between items-center text-xs text-gray-400 mb-1.5">
              <span className="bg-blue-50 text-blue-600 px-2 py-0.5 rounded font-medium">
                {board.category}
              </span>
              <span>{board.writer}</span>
            </div>
            <h3 className="font-bold text-gray-800 text-base line-clamp-1">
              {board.title}
            </h3>
            <p className="text-sm text-gray-500 line-clamp-2 mt-1">
              {board.content}
            </p>
            <div className="mt-3 text-xs text-gray-400 flex gap-4">
              <span>조회 {board.viewCount}</span>
              <span>좋아요 {board.likeCount}</span>
              <span className="ml-auto">
                {board.createdAt?.substring(0, 10)}
              </span>
            </div>
          </div>
        ))}
      </div>

      {/* 페이징 컨트롤 */}
      <div className="flex justify-between items-center mt-6 pt-4 border-t border-gray-100">
        <button
          disabled={page === 0}
          onClick={() => setPage((p) => p - 1)}
          className="px-3 py-1.5 text-xs bg-gray-100 rounded-lg disabled:opacity-40 font-medium"
        >
          이전
        </button>
        <span className="text-xs font-semibold text-gray-600">
          {page + 1} / {boardPage.totalPages || 1}
        </span>
        <button
          disabled={page + 1 >= boardPage.totalPages}
          onClick={() => setPage((p) => p + 1)}
          className="px-3 py-1.5 text-xs bg-gray-100 rounded-lg disabled:opacity-40 font-medium"
        >
          다음
        </button>
      </div>
    </div>
  );
}
