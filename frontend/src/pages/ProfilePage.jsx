import React, { useEffect, useState } from "react";
import api from "../api/axiosInstance";
import { useAuthStore } from "../stores/useAuthStore";

export function ProfilePage({ onNavigate }) {
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const { logout } = useAuthStore();

  useEffect(() => {
    fetchProfile();
  }, []);

  const fetchProfile = async () => {
    try {
      const res = await api.get("/api/profile");
      const data = res.data.data || res.data;
      setProfile(data);
    } catch (err) {
      alert("프로필 정보를 불러오는 데 실패했습니다.");
    } finally {
      setLoading(false);
    }
  };

  // 회원 탈퇴 처리 (DELETE /api/auth/withdraw)
  const handleWithdraw = async () => {
    if (
      !window.confirm(
        "정말로 탈퇴하시겠습니까? 탈퇴 후에는 계정을 복구할 수 없습니다.",
      )
    )
      return;

    try {
      await api.delete("/api/auth/withdraw");
      alert("회원 탈퇴가 완료되었습니다.");
      await logout();
      onNavigate("boardList");
    } catch (err) {
      alert(
        "회원 탈퇴 실패: " +
          (err.response?.data?.message || "오류가 발생했습니다."),
      );
    }
  };

  if (loading)
    return <div className="p-4 text-center text-gray-500">로딩 중...</div>;
  if (!profile)
    return (
      <div className="p-4 text-center text-gray-500">
        프로필 정보를 찾을 수 없습니다.
      </div>
    );

  return (
    <div className="py-2 space-y-4">
      <h2 className="text-xl font-bold text-gray-800 mb-4">내 프로필</h2>

      <div className="bg-white p-5 rounded-2xl border border-gray-100 shadow-sm space-y-4">
        <div>
          <label className="block text-xs font-bold text-gray-400 mb-1">
            이름
          </label>
          <p className="text-base font-semibold text-gray-800">
            {profile.name || "-"}
          </p>
        </div>

        <div className="border-t border-gray-100 pt-3">
          <label className="block text-xs font-bold text-gray-400 mb-1">
            닉네임
          </label>
          <p className="text-base font-semibold text-gray-800">
            {profile.nickName || profile.nickname || "-"}
          </p>
        </div>

        <div className="border-t border-gray-100 pt-3">
          <label className="block text-xs font-bold text-gray-400 mb-1">
            이메일
          </label>
          <p className="text-base font-semibold text-gray-800">
            {profile.email || "-"}
          </p>
        </div>
      </div>

      <div className="space-y-2 pt-2">
        <button
          onClick={() => onNavigate("boardList")}
          className="w-full bg-gray-100 text-gray-600 py-3 rounded-xl font-bold text-sm hover:bg-gray-200 transition-colors"
        >
          목록으로 돌아가기
        </button>

        <button
          onClick={handleWithdraw}
          className="w-full bg-red-50 text-red-500 py-3 rounded-xl font-bold text-xs hover:bg-red-100 transition-colors border border-red-100"
        >
          회원 탈퇴
        </button>
      </div>
    </div>
  );
}
