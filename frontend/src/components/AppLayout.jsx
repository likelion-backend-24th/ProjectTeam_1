import React from "react";
import { useAuthStore } from "../stores/useAuthStore";

export function AppLayout({ children, currentView, onNavigate }) {
  const { accessToken } = useAuthStore();

  const handleProfileClick = () => {
    // 비로그인 상태에서 프로필 탭을 누르면 로그인 화면으로 이동
    if (!accessToken) {
      onNavigate("login");
    } else {
      onNavigate("profile");
    }
  };

  return (
    <div className="min-h-screen bg-gray-100 flex justify-center items-center py-0 sm:py-6">
      {/* 모바일 컨테이너 프레임 */}
      <div className="w-full max-w-[430px] h-[100vh] sm:h-[880px] bg-white sm:rounded-[36px] shadow-2xl flex flex-col overflow-hidden relative border border-gray-100">
        {/* 상단 상태바 시뮬레이션 */}
        <div className="px-6 pt-3 pb-1 flex justify-between items-center text-xs font-bold text-gray-800 select-none bg-white z-10">
          <span>9:41</span>
          <div className="flex items-center space-x-1.5">
            <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 24 24">
              <path d="M12 3c-4.97 0-9 4.03-9 9 0 2.12.74 4.07 1.97 5.61L4.35 19.4c-.39.39-.39 1.02 0 1.41.39.39 1.02.39 1.41 0l1.9-1.9C9.22 19.54 10.57 20 12 20c4.97 0 9-4.03 9-9s-4.03-9-9-9z" />
            </svg>
            <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 24 24">
              <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8z" />
            </svg>
          </div>
        </div>

        {/* 페이지 컨텐츠 영역 */}
        <main className="flex-1 overflow-y-auto bg-white pb-16">
          {children}
        </main>

        {/* 하단 탭 바 (게시글, 프로필 2개만 유지) */}
        <nav className="absolute bottom-0 left-0 right-0 h-16 bg-white border-t border-gray-100 flex justify-around items-center px-6 z-20">
          {/* 게시글 탭 */}
          <button
            onClick={() => onNavigate("boardList")}
            className={`flex flex-col items-center p-2 transition-colors ${
              currentView === "boardList" ||
              currentView === "boardDetail" ||
              currentView === "boardWrite"
                ? "text-black"
                : "text-gray-400"
            }`}
          >
            <svg
              className="w-6 h-6"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                d="M19 20H5a2 2 0 01-2-2V6a2 2 0 012-2h10a2 2 0 012 2v1m2 13a2 2 0 01-2-2V7m2 13a2 2 0 002-2V9a2 2 0 00-2-2h-2m-4-3H9M7 16h6M7 8h6v4H7V8z"
              />
            </svg>
            <span className="text-[10px] font-bold mt-0.5">게시글</span>
          </button>

          {/* 프로필 탭 */}
          <button
            onClick={handleProfileClick}
            className={`flex flex-col items-center p-2 transition-colors ${
              currentView === "profile" ||
              currentView === "login" ||
              currentView === "signup"
                ? "text-black"
                : "text-gray-400"
            }`}
          >
            <svg
              className="w-6 h-6"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"
              />
            </svg>
            <span className="text-[10px] font-bold mt-0.5">프로필</span>
          </button>
        </nav>
      </div>
    </div>
  );
}
