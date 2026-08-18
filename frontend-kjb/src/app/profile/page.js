"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { AppShell, PageHeader } from "@/components/AppShell";
import { RequireAuth } from "@/components/RequireAuth";
import { useAuthStore } from "@/store/authStore";
import { useToastStore } from "@/store/toastStore";
import { ApiError } from "@/lib/api/client";

function ProfileView() {
  const profile = useAuthStore((s) => s.profile);
  const logout = useAuthStore((s) => s.logout);
  const withdraw = useAuthStore((s) => s.withdraw);
  const updateProfile = useAuthStore((s) => s.updateProfile); // 닉네임 수정용 액션 (스토어에 구현되어 있다고 가정)
  const showToast = useToastStore((s) => s.showToast);
  const router = useRouter();

  const [isWithdrawing, setIsWithdrawing] = useState(false);
  
  // 프로필 수정 상태 관리
  const [isEditing, setIsEditing] = useState(false);
  const [nickNameInput, setNickNameInput] = useState(profile?.nickName ?? "");
  const [isSaving, setIsSaving] = useState(false);

  async function handleLogout() {
    await logout();
    router.replace("/login");
  }

  async function handleWithdraw() {
    if (!window.confirm("정말 탈퇴하시겠어요? 이 작업은 되돌릴 수 없어요.")) return;

    setIsWithdrawing(true);
    try {
      await withdraw();
      showToast("탈퇴가 완료되었어요.");
      router.replace("/login");
    } catch (err) {
      showToast(err instanceof ApiError ? err.message : "탈퇴에 실패했어요. 다시 시도해주세요.", "error");
    } finally {
      setIsWithdrawing(false);
    }
  }

  // 수정 모드 진입
  function handleStartEdit() {
    setNickNameInput(profile?.nickName ?? "");
    setIsEditing(true);
  }

  // 수정 취소
  function handleCancelEdit() {
    setIsEditing(false);
    setNickNameInput(profile?.nickName ?? "");
  }

  // 닉네임 저장
  async function handleSaveProfile() {
    console.log("1. 저장 버튼 클릭됨, 입력값:", nickNameInput);

    if (!nickNameInput.trim()) {
      showToast("닉네임을 입력해주세요.", "error");
      return;
    }

    setIsSaving(true);
    try {
      console.log("2. API 호출 직전");
      await updateProfile({ nickName: nickNameInput });
      console.log("3. API 호출 성공");
      
      showToast("닉네임이 수정되었어요.");
      setIsEditing(false);
    } catch (err) {
      console.error("4. API 호출 에러 발생:", err);
      showToast(err instanceof ApiError ? err.message : "닉네임 수정에 실패했어요.", "error");
    } finally {
      setIsSaving(false);
    }
  }

  return (
    <AppShell header={<PageHeader title="내 프로필" />}>
      <div className="flex justify-center pt-3 pb-1">
        <div className="flex h-[76px] w-[76px] items-center justify-center rounded-full bg-surface-strong text-[28px] font-bold text-ink-soft">
          {profile?.name?.slice(0, 1) ?? "?"}
        </div>
      </div>

      <ReadonlyField label="이름" value={profile?.name} />

      {/* 닉네임 영역 (읽기 모드 vs 수정 모드) */}
      <div className="flex flex-col gap-2">
        <div className="flex items-center justify-between">
          <span className="text-sm font-semibold">닉네임</span>
          {!isEditing ? (
            <button
              type="button"
              onClick={handleStartEdit}
              className="text-xs font-medium text-primary hover:underline"
            >
              프로필 수정
            </button>
          ) : (
            <div className="flex gap-2">
              <button
                type="button"
                onClick={handleCancelEdit}
                disabled={isSaving}
                className="text-xs font-medium text-ink-muted"
              >
                취소
              </button>
              <button
                type="button"
                onClick={handleSaveProfile}
                disabled={isSaving}
                className="text-xs font-semibold text-primary"
              >
                {isSaving ? "저장 중..." : "저장"}
              </button>
            </div>
          )}
        </div>

        {!isEditing ? (
          <div className="w-full rounded-xl bg-surface px-4 py-3.5 text-[15px]">
            {profile?.nickName}
          </div>
        ) : (
          <input
            type="text"
            value={nickNameInput}
            onChange={(e) => setNickNameInput(e.target.value)}
            placeholder="변경할 닉네임을 입력하세요"
            className="w-full rounded-xl bg-surface px-4 py-3.5 text-[15px] outline-none ring-2 ring-primary focus:ring-2"
            autoFocus
          />
        )}
      </div>

      <ReadonlyField label="이메일" value={profile?.email} />

      <button
        type="button"
        onClick={handleLogout}
        className="mt-2 h-[50px] w-full rounded-xl bg-surface text-[15px] font-semibold text-ink"
      >
        로그아웃
      </button>

      <button
        type="button"
        onClick={handleWithdraw}
        disabled={isWithdrawing}
        className="h-[46px] w-full text-[13px] font-medium text-ink-muted disabled:opacity-50"
      >
        {isWithdrawing ? "탈퇴 처리 중..." : "회원 탈퇴"}
      </button>
    </AppShell>
  );
}

function ReadonlyField({ label, value }) {
  return (
    <div className="flex flex-col gap-2">
      <span className="text-sm font-semibold">{label}</span>
      <div className="w-full rounded-xl bg-surface px-4 py-3.5 text-[15px]">{value}</div>
    </div>
  );
}

export default function ProfilePage() {
  return (
    <RequireAuth>
      <ProfileView />
    </RequireAuth>
  );
}