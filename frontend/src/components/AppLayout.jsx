import React from "react";
import { useAuthStore } from "../stores/useAuthStore";

export default function AppLayout({ children, currentView, onNavigate }) {
  const { accessToken, logout } = useAuthStore();

  const handleLogout = async () => {
    await logout();
    alert("로그아웃되었습니다.");
    onNavigate("boardList");
  };

  return (
    <div className="min-h-screen bg-gray-100 flex justify-center items-center font-sans">
      <div className="w-full max-w-[430px] min-h-screen bg-white shadow-2xl flex flex-col relative border-x border-gray-200">
        {/* Header Nav */}
        <header className="h-14 border-b border-gray-100 flex items-center justify-between px-4 bg-white sticky top-0 z-10">
          <h1
            onClick={() => onNavigate("boardList")}
            className="font-black text-lg text-blue-600 cursor-pointer"
          >
            CITYFARM
          </h1>
          <div className="flex gap-1 text-xs items-center">
            <button
              onClick={() => onNavigate("boardList")}
              className={`px-2 py-1 font-medium ${currentView === "boardList" ? "text-blue-600 font-bold" : "text-gray-600"}`}
            >
              게시판
            </button>

            {/* 로그인 시 마이페이지 & 로그아웃 표시 */}
            {accessToken ? (
              <>
                <button
                  onClick={() => onNavigate("profile")}
                  className={`px-2 py-1 font-bold ${currentView === "profile" ? "text-blue-600 font-bold" : "text-gray-600"}`}
                >
                  마이페이지
                </button>
                <button
                  onClick={() => onNavigate("admin")}
                  className={`px-2 py-1 font-bold ${currentView === "admin" ? "text-red-600" : "text-red-400"}`}
                >
                  관리자
                </button>
                <button
                  onClick={handleLogout}
                  className="px-2 py-1 text-gray-500 font-bold hover:text-gray-800"
                >
                  로그아웃
                </button>
              </>
            ) : (
              <button
                onClick={() => onNavigate("login")}
                className="px-2 py-1 text-blue-600 font-bold hover:underline"
              >
                로그인
              </button>
            )}
          </div>
        </header>

        {/* Main Content */}
        <main className="flex-1 p-4 pb-10 overflow-y-auto">{children}</main>
      </div>
    </div>
  );
}
