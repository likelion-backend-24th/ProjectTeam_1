import React, { useEffect } from "react";
import AppLayout from "./components/AppLayout";
import { LoginPage } from "./pages/LoginPage";
import { SignupPage } from "./pages/SignupPage";
import { BoardListPage } from "./pages/BoardListPage";
import { BoardDetailPage } from "./pages/BoardDetailPage";
import { BoardWritePage } from "./pages/BoardWritePage";
import { AdminUserPage } from "./pages/AdminUserPage";
import { ProfilePage } from "./pages/ProfilePage";
import { useAuthStore } from "./stores/useAuthStore";
import api from "./api/axiosInstance";

export default function App() {
  const [currentView, setCurrentView] = React.useState("boardList");
  const [selectedBoardId, setSelectedBoardId] = React.useState(null);
  const { accessToken, logout } = useAuthStore();

  // 앱 시작 시 토큰 유효성 검사 (실패 시 안전하게 로그아웃)
  useEffect(() => {
    if (accessToken) {
      api.get("/api/profile").catch((err) => {
        console.warn("토큰이 유효하지 않아 초기화합니다.", err);
        logout();
      });
    }
  }, []);

  const handleNavigate = (view) => {
    setCurrentView(view);
  };

  const handleSelectBoard = (boardId) => {
    setSelectedBoardId(boardId);
    setCurrentView("boardDetail");
  };

  return (
    <AppLayout currentView={currentView} onNavigate={handleNavigate}>
      {currentView === "login" && <LoginPage onNavigate={handleNavigate} />}
      {currentView === "signup" && <SignupPage onNavigate={handleNavigate} />}
      {currentView === "boardList" && (
        <BoardListPage
          onSelectBoard={handleSelectBoard}
          onNavigate={handleNavigate}
        />
      )}
      {currentView === "boardDetail" && (
        <BoardDetailPage
          boardId={selectedBoardId}
          onNavigate={handleNavigate}
        />
      )}
      {currentView === "boardWrite" && (
        <BoardWritePage onNavigate={handleNavigate} />
      )}
      {currentView === "admin" && <AdminUserPage />}
      {currentView === "profile" && <ProfilePage onNavigate={handleNavigate} />}
    </AppLayout>
  );
}
