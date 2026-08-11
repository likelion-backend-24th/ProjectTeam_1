import React, { useEffect, useState, useCallback } from "react";
import { useAuthStore } from "../stores/useAuthStore";
import api from "../api/axiosInstance";

const SEARCH_TYPES = [
  { value: "title", label: "제목" },
  { value: "content", label: "내용" },
  { value: "author", label: "작성자" },
];

// 백엔드 Enum (FREE, QNA, NOTICE) 반영
const CATEGORIES = [
  { value: "", label: "전체" },
  { value: "NOTICE", label: "공지" },
  { value: "QNA", label: "질문" },
  { value: "FREE", label: "자유" },
];

export function BoardListPage({ onNavigate, onSelectPost }) {
  const { roleType } = useAuthStore();
  const isAdmin = roleType === "ADMIN";

  const [posts, setPosts] = useState([]);
  const [selectedCategory, setSelectedCategory] = useState("");
  const [searchType, setSearchType] = useState("title");
  const [searchKeyword, setSearchKeyword] = useState("");
  const [appliedType, setAppliedType] = useState("title");
  const [appliedKeyword, setAppliedKeyword] = useState("");

  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");

  const fetchPosts = useCallback(async () => {
    try {
      setLoading(true);
      setErrorMessage("");

      const params = {
        page: page,
        size: 10,
        sort: "createdAt,desc",
      };

      if (selectedCategory && selectedCategory.trim() !== "") {
        params.category = selectedCategory.trim();
      }

      const trimmedKeyword = appliedKeyword.trim();
      if (trimmedKeyword !== "") {
        params.keyword = trimmedKeyword;
        params.type = appliedType || "title";
      }

      const res = await api.get("/api/board", { params });
      const data = res.data.data || res.data;

      if (data && data.content) {
        setPosts(data.content);
        setTotalPages(data.totalPages || 0);
        setTotalElements(data.totalElements || 0);
      } else if (Array.isArray(data)) {
        setPosts(data);
        setTotalPages(1);
        setTotalElements(data.length);
      } else {
        setPosts([]);
        setTotalPages(0);
        setTotalElements(0);
      }
    } catch (err) {
      console.error("게시글 목록 조회 실패:", err);
      setErrorMessage("게시글을 불러오는 중에 오류가 발생했습니다.");
    } finally {
      setLoading(false);
    }
  }, [selectedCategory, appliedKeyword, appliedType, page]);

  useEffect(() => {
    fetchPosts();
  }, [fetchPosts]);

  const handleCategoryChange = (catValue) => {
    setSelectedCategory(catValue);
    setPage(0);
  };

  const handleSearchSubmit = (e) => {
    e.preventDefault();
    const nextKeyword = searchKeyword.trim();

    if (
      page === 0 &&
      appliedKeyword === nextKeyword &&
      appliedType === searchType
    ) {
      fetchPosts();
    } else {
      setAppliedType(searchType);
      setAppliedKeyword(nextKeyword);
      setPage(0);
    }
  };

  const handleResetSearch = () => {
    setSearchKeyword("");
    setAppliedKeyword("");
    setSearchType("title");
    setAppliedType("title");
    setPage(0);
  };

  const renderPageNumbers = () => {
    const pageNumbers = [];
    const maxVisiblePages = 5;
    let startPage = Math.max(0, page - Math.floor(maxVisiblePages / 2));
    let endPage = Math.min(totalPages - 1, startPage + maxVisiblePages - 1);

    if (endPage - startPage + 1 < maxVisiblePages) {
      startPage = Math.max(0, endPage - maxVisiblePages + 1);
    }

    for (let i = startPage; i <= endPage; i++) {
      pageNumbers.push(
        <button
          key={i}
          onClick={() => setPage(i)}
          className={`w-8 h-8 rounded-lg text-xs font-bold transition-all ${
            page === i
              ? "bg-gray-900 text-white shadow-sm"
              : "bg-gray-100 text-gray-600 hover:bg-gray-200"
          }`}
        >
          {i + 1}
        </button>,
      );
    }
    return pageNumbers;
  };

  return (
    <div className="p-4 max-w-2xl mx-auto space-y-4">
      <div className="flex items-center justify-between pt-1 pb-2">
        <div>
          <h1 className="text-xl font-bold text-gray-900">게시판</h1>
          <p className="text-xs text-gray-400 mt-0.5">
            총 {totalElements}개의 게시글
          </p>
        </div>
        <button
          onClick={() => onNavigate("boardWrite")}
          className="px-4 py-2 bg-gray-900 text-white font-bold text-xs rounded-xl hover:bg-black transition-colors shadow-sm flex items-center space-x-1.5"
        >
          <span>✏️</span>
          <span>글쓰기</span>
        </button>
      </div>

      <div className="flex border-b border-gray-200 text-xs font-bold">
        {CATEGORIES.map((cat) => (
          <button
            key={cat.value}
            onClick={() => handleCategoryChange(cat.value)}
            className={`py-2.5 px-4 transition-colors border-b-2 -mb-px ${
              selectedCategory === cat.value
                ? "border-gray-900 text-gray-900 font-extrabold"
                : "border-transparent text-gray-400 hover:text-gray-600"
            }`}
          >
            {cat.label}
          </button>
        ))}
      </div>

      <form
        onSubmit={handleSearchSubmit}
        className="flex items-center gap-2 pt-1"
      >
        <select
          value={searchType}
          onChange={(e) => setSearchType(e.target.value)}
          className="p-2.5 bg-gray-50 border border-gray-200 rounded-xl text-xs font-bold text-gray-700 focus:outline-none focus:border-gray-400"
        >
          {SEARCH_TYPES.map((type) => (
            <option key={type.value} value={type.value}>
              {type.label}
            </option>
          ))}
        </select>

        <div className="relative flex-1">
          <input
            type="text"
            value={searchKeyword}
            onChange={(e) => setSearchKeyword(e.target.value)}
            placeholder="검색어를 입력하세요..."
            className="w-full p-2.5 pr-8 bg-gray-50 border border-gray-200 rounded-xl text-xs font-medium text-gray-800 focus:outline-none focus:border-gray-400"
          />
          {searchKeyword && (
            <button
              type="button"
              onClick={() => setSearchKeyword("")}
              className="absolute right-2.5 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600 text-xs font-bold"
            >
              ✕
            </button>
          )}
        </div>

        <button
          type="submit"
          className="px-4 py-2.5 bg-gray-800 text-white font-bold text-xs rounded-xl hover:bg-gray-900 transition-colors whitespace-nowrap"
        >
          검색
        </button>
      </form>

      {appliedKeyword && (
        <div className="flex items-center justify-between px-3 py-2 bg-blue-50 border border-blue-100 rounded-xl text-xs text-blue-700">
          <span>
            <b>'{appliedKeyword}'</b> 검색 결과입니다.
          </span>
          <button
            onClick={handleResetSearch}
            className="text-blue-500 font-bold hover:underline ml-2"
          >
            검색 초기화
          </button>
        </div>
      )}

      {loading ? (
        <div className="py-12 text-center text-xs text-gray-400 font-medium">
          게시글을 불러오는 중입니다...
        </div>
      ) : errorMessage ? (
        <div className="py-12 text-center text-xs text-red-500 font-medium bg-red-50 rounded-2xl border border-red-100">
          {errorMessage}
        </div>
      ) : posts.length === 0 ? (
        <div className="py-12 text-center text-xs text-gray-400 font-medium bg-gray-50 rounded-2xl border border-dashed border-gray-200">
          게시글이 존재하지 않습니다.
        </div>
      ) : (
        <div className="space-y-2.5">
          {posts.map((post) => {
            const postId = post.id ?? post.boardId;
            const isNotice = post.category === "NOTICE";
            const isQna = post.category === "QNA";

            return (
              <div
                key={postId}
                onClick={() => onSelectPost(postId)}
                className={`p-4 border rounded-2xl shadow-sm transition-all cursor-pointer space-y-2 ${
                  isNotice
                    ? "bg-red-50/30 border-red-100 hover:border-red-200"
                    : "bg-white border-gray-100 hover:border-gray-300"
                }`}
              >
                <div className="flex items-center justify-between text-[11px]">
                  <span
                    className={`px-2 py-0.5 rounded font-bold ${
                      isNotice
                        ? "bg-red-100 text-red-600"
                        : isQna
                          ? "bg-blue-100 text-blue-600"
                          : "bg-gray-100 text-gray-600"
                    }`}
                  >
                    {isNotice ? "공지" : isQna ? "질문" : "자유"}
                  </span>
                  <span className="text-gray-400">
                    {post.createdAt
                      ? new Date(post.createdAt).toLocaleDateString()
                      : ""}
                  </span>
                </div>

                <h2 className="text-sm font-bold text-gray-800 line-clamp-1">
                  {post.title}
                </h2>

                <div className="flex items-center justify-between text-xs text-gray-400 pt-1">
                  <span className="font-medium text-gray-600">
                    {post.writer || "익명"}
                  </span>
                  <div className="flex items-center space-x-3 text-[11px]">
                    <span>👁 {post.viewCount ?? 0}</span>
                    <span>❤️ {post.likeCount ?? 0}</span>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {totalPages > 1 && (
        <div className="flex justify-center items-center space-x-1.5 pt-4">
          <button
            disabled={page === 0}
            onClick={() => setPage((prev) => Math.max(prev - 1, 0))}
            className="px-3 py-1.5 bg-gray-100 rounded-lg text-xs font-bold text-gray-600 disabled:opacity-40 hover:bg-gray-200 transition-colors"
          >
            이전
          </button>

          {renderPageNumbers()}

          <button
            disabled={page >= totalPages - 1}
            onClick={() =>
              setPage((prev) => Math.min(prev + 1, totalPages - 1))
            }
            className="px-3 py-1.5 bg-gray-100 rounded-lg text-xs font-bold text-gray-600 disabled:opacity-40 hover:bg-gray-200 transition-colors"
          >
            다음
          </button>
        </div>
      )}
    </div>
  );
}
