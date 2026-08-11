import React, { useEffect, useState } from "react";
import { useAuthStore } from "../stores/useAuthStore";
import api from "../api/axiosInstance";

export function ProfilePage({ onNavigate }) {
  const { accessToken, name, nickName, email, roleType, setProfile, logout } =
    useAuthStore();
  const [loading, setLoading] = useState(true);

  const isAdmin = roleType === "ADMIN";

  useEffect(() => {
    if (!accessToken) {
      alert("로그인이 필요합니다.");
      onNavigate("login");
      return;
    }

    const fetchProfile = async () => {
      try {
        setLoading(true);
        // GET /api/profile
        const res = await api.get("/api/profile");
        const data = res.data.data || res.data;

        // name(성명), nickName(활동명), email(가입계정명) 업데이트
        setProfile({
          name: data.name,
          nickName: data.nickName,
          email: data.email,
        });
      } catch (err) {
        console.error("프로필 조회 실패:", err);
      } finally {
        setLoading(false);
      }
    };

    fetchProfile();
  }, [accessToken]);

  const handleLogout = () => {
    logout();
    alert("로그아웃되었습니다.");
    onNavigate("login");
  };

  return (
    <div className="p-4 space-y-6">
      <div className="flex items-center justify-between pt-1 border-b border-gray-100 pb-3">
        <h1 className="text-base font-bold text-gray-900">내 프로필</h1>
        <button
          onClick={handleLogout}
          className="text-xs font-bold text-red-500 hover:text-red-700"
        >
          로그아웃
        </button>
      </div>

      <div className="flex justify-center py-2">
        <div className="w-24 h-24 rounded-full bg-blue-100 flex items-center justify-center text-2xl font-bold text-blue-600">
          {nickName ? nickName[0].toUpperCase() : "U"}
        </div>
      </div>

      <div className="space-y-4 pt-2">
        <div>
          <label className="block text-xs font-bold text-gray-400 mb-1.5">
            활동명 (닉네임)
          </label>
          <div className="w-full p-3 bg-gray-50 border border-gray-200 rounded-xl text-xs font-bold text-gray-800">
            {loading ? "불러오는 중..." : nickName || "-"}
          </div>
        </div>

        <div>
          <label className="block text-xs font-bold text-gray-400 mb-1.5">
            성명 / 가입 계정
          </label>
          <div className="w-full p-3 bg-gray-50 border border-gray-200 rounded-xl text-xs font-bold text-gray-800">
            {loading
              ? "불러오는 중..."
              : `${name || "미등록"} (${email || "-"})`}
          </div>
        </div>

        <div>
          <label className="block text-xs font-bold text-gray-400 mb-1.5">
            계정 권한 (JWT role)
          </label>
          <div className="w-full p-3 bg-gray-50 border border-gray-200 rounded-xl text-xs font-bold text-gray-800 flex justify-between items-center">
            <span>{isAdmin ? "관리자 (ADMIN)" : "일반 회원 (USER)"}</span>
            {isAdmin && (
              <span className="px-2 py-0.5 bg-red-100 text-red-600 rounded text-[10px] font-bold">
                ADMIN
              </span>
            )}
          </div>
        </div>
      </div>

      {/* {isAdmin && (
        <div className="pt-2">
          <button
            onClick={() => onNavigate("admin")}
            className="w-full py-3.5 bg-gray-900 text-white font-bold text-xs rounded-xl hover:bg-black transition-colors shadow-sm"
          >
            🛠 관리자 페이지로 이동
          </button>
        </div>
      )} */}

      <div className="pt-4 border-t border-gray-100">
        <button
          onClick={async () => {
            if (confirm("정말로 탈퇴하시겠습니까?")) {
              try {
                await api.delete("/api/auth/withdraw");
                alert("탈퇴되었습니다.");
              } catch (err) {
                console.error("탈퇴 처리 실패:", err);
              } finally {
                logout();
                onNavigate("login");
              }
            }
          }}
          className="w-full py-3 bg-gray-100 text-gray-500 font-bold text-xs rounded-xl hover:bg-red-50 hover:text-red-600 transition-colors"
        >
          탈퇴하기
        </button>
      </div>
    </div>
  );
}
