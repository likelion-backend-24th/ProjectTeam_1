import React, { useState } from "react";
import { BoardListPage } from "./pages/BoardListPage";
import { BoardDetailPage } from "./pages/BoardDetailPage";
import { LoginPage } from "./pages/LoginPage";
import { ProfilePage } from "./pages/ProfilePage";
import { BoardWritePage } from "./pages/BoardWritePage";
import { AdminUserPage } from "./pages/AdminUserPage";
import { useAuthStore } from "./stores/useAuthStore";

export default function App() {
  const [currentPage, setCurrentPage] = useState("boardList");
  const [selectedPostId, setSelectedPostId] = useState(null);

  const { roleType } = useAuthStore();
  const isAdmin = roleType === "ADMIN";

  const handleNavigate = (page) => {
    setCurrentPage(page);
  };

  const handleSelectPost = (postId) => {
    if (!postId) {
      alert("게시글 ID가 유효하지 않습니다.");
      return;
    }
    setSelectedPostId(postId);
    setCurrentPage("boardDetail");
  };

  return (
    <div className="min-h-screen bg-gray-100 flex items-center justify-center p-2 sm:p-4">
      {/* 1. 스마트폰 프레임 높이를 h-[840px]로 고정 */}
      <div className="w-full max-w-[420px] h-[840px] bg-white rounded-[32px] shadow-2xl border border-gray-200 overflow-hidden flex flex-col relative">
        {/* 2. 스크롤 가능한 메인 영역 (투명 스크롤바 적용) */}
        <div className="flex-1 overflow-y-auto pb-16 [scrollbar-width:none] [&::-webkit-scrollbar]:hidden">
          {currentPage === "boardList" && (
            <BoardListPage
              onNavigate={handleNavigate}
              onSelectPost={handleSelectPost}
            />
          )}

          {currentPage === "boardDetail" && (
            <BoardDetailPage
              postId={selectedPostId}
              onNavigate={handleNavigate}
            />
          )}

          {currentPage === "boardWrite" && (
            <BoardWritePage onNavigate={handleNavigate} />
          )}

          {currentPage === "login" && <LoginPage onNavigate={handleNavigate} />}

          {currentPage === "profile" && (
            <ProfilePage onNavigate={handleNavigate} />
          )}

          {currentPage === "adminUser" && (
            <AdminUserPage onNavigate={handleNavigate} />
          )}
        </div>

        {/* 3. 하단 고정 네비게이션 바 */}
        <nav className="absolute bottom-0 left-0 right-0 h-14 bg-white/95 backdrop-blur-md border-t border-gray-100 flex justify-around items-center px-6 z-10">
          <button
            onClick={() => handleNavigate("boardList")}
            className={`flex flex-col items-center space-y-0.5 text-[11px] font-bold ${
              currentPage === "boardList" || currentPage === "boardDetail"
                ? "text-gray-900"
                : "text-gray-400 hover:text-gray-600"
            }`}
          >
            <span className="text-base">📑</span>
            <span>게시글</span>
          </button>

          {isAdmin && (
            <button
              onClick={() => handleNavigate("adminUser")}
              className={`flex flex-col items-center space-y-0.5 text-[11px] font-bold ${
                currentPage === "adminUser"
                  ? "text-gray-900"
                  : "text-gray-400 hover:text-gray-600"
              }`}
            >
              <span className="text-base">⚙️</span>
              <span>회원관리</span>
            </button>
          )}

          <button
            onClick={() => handleNavigate("profile")}
            className={`flex flex-col items-center space-y-0.5 text-[11px] font-bold ${
              currentPage === "profile"
                ? "text-gray-900"
                : "text-gray-400 hover:text-gray-600"
            }`}
          >
            <span className="text-base">👤</span>
            <span>프로필</span>
          </button>
        </nav>
      </div>
    </div>
  );
}
