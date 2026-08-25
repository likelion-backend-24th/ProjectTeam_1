"use client";

import { useEffect, useState } from "react";
import LinkComponent from "next/link";
import { useRouter } from "next/navigation";
import { AppShell, PageHeader } from "@/components/AppShell";
import { RequireAuth } from "@/components/RequireAuth";
import { ChevronRightIcon, HeartIcon, ReceiptIcon, TicketIcon, LockIcon, BackIcon } from "@/components/icons";
import { useAuthStore } from "@/store/authStore";
import { useToastStore } from "@/store/toastStore";
import { ApiError } from "@/lib/api/client";
import { getFollowerList, getFollowingList } from "@/lib/api/follow";

function ProfileView() {
  const profile = useAuthStore((s) => s.profile);
  const logout = useAuthStore((s) => s.logout);
  const withdraw = useAuthStore((s) => s.withdraw);
  const updateProfile = useAuthStore((s) => s.updateProfile);
  const checkPassword = useAuthStore((s) => s.checkPassword);
  const changePassword = useAuthStore((s) => s.changePassword);
  const showToast = useToastStore((s) => s.showToast);
  const router = useRouter();

  const [isWithdrawing, setIsWithdrawing] = useState(false);
  
  // 프로필 수정 상태 관리
  const [isEditing, setIsEditing] = useState(false);
  const [nicknameInput, setNicknameInput] = useState(profile?.nickname ?? "");
  const [isSaving, setIsSaving] = useState(false);

  // 비밀번호 변경 모달 상태 관리
  const [isPasswordModalOpen, setIsPasswordModalOpen] = useState(false);
  const [passwordStep, setPasswordStep] = useState(1); // 1: 현재 비밀번호 확인, 2: 새 비밀번호 변경
  const [currentPasswordInput, setCurrentPasswordInput] = useState("");
  const [newPasswordInput, setNewPasswordInput] = useState("");
  const [isPasswordLoading, setIsPasswordLoading] = useState(false);

  // 팔로워/팔로잉 카운트
  const [followerCount, setFollowerCount] = useState(null);
  const [followingCount, setFollowingCount] = useState(null);

  useEffect(() => {
    let cancelled = false;
    Promise.all([getFollowerList(), getFollowingList()])
      .then(([followers, following]) => {
        if (cancelled) return;
        setFollowerCount(followers.length);
        setFollowingCount(following.length);
      })
      .catch(() => {});
    return () => {
      cancelled = true;
    };
  }, []);

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

  function handleStartEdit() {
    setNicknameInput(profile?.nickname ?? "");
    setIsEditing(true);
  }

  function handleCancelEdit() {
    setIsEditing(false);
    setNicknameInput(profile?.nickname ?? "");
  }

  async function handleSaveProfile() {
    if (!nicknameInput.trim()) {
      showToast("닉네임을 입력해주세요.", "error");
      return;
    }

    setIsSaving(true);
    try {
      await updateProfile({ nickname: nicknameInput });
      showToast("닉네임이 수정되었어요.");
      setIsEditing(false);
    } catch (err) {
      showToast(err instanceof ApiError ? err.message : "닉네임 수정에 실패했어요.", "error");
    } finally {
      setIsSaving(false);
    }
  }

  // 1단계: 현재 비밀번호 확인 핸들러
  async function handleVerifyCurrentPassword(e) {
    e.preventDefault();
    if (!currentPasswordInput) {
      showToast("현재 비밀번호를 입력해주세요.", "error");
      return;
    }

    setIsPasswordLoading(true);
    try {
      await checkPassword({ currentPassword: currentPasswordInput });
      setPasswordStep(2);
      setCurrentPasswordInput("");
    } catch (err) {
      showToast(err instanceof ApiError ? err.message : "현재 비밀번호가 일치하지 않아요.", "error");
    } finally {
      setIsPasswordLoading(false);
    }
  }

  // 2단계: 새 비밀번호 변경 핸들러
  async function handleChangeNewPassword(e) {
    e.preventDefault();
    if (!newPasswordInput) {
      showToast("새 비밀번호를 입력해주세요.", "error");
      return;
    }

    setIsPasswordLoading(true);
    try {
      await changePassword({ newPassword: newPasswordInput });
      showToast("비밀번호가 성공적으로 변경되었어요.");
      closePasswordModal();
    } catch (err) {
      showToast(err instanceof ApiError ? err.message : "비밀번호 변경에 실패했어요.", "error");
    } finally {
      setIsPasswordLoading(false);
    }
  }

  function closePasswordModal() {
    setIsPasswordModalOpen(false);
    setPasswordStep(1);
    setCurrentPasswordInput("");
    setNewPasswordInput("");
  }

  return (
    <AppShell
      header={
        <PageHeader
          title="내 프로필"
          left={
            <button type="button" onClick={() => router.back()} className="cursor-pointer">
              <BackIcon size={22} />
            </button>
          }
        />
      }
    >
     <div className="relative flex flex-col gap-1 overflow-hidden rounded-[18px] bg-gradient-to-br from-[#1a2e22] via-[#2f5138] to-[#3f6b48] px-5 py-[22px] text-white">
        <span className="text-xs font-bold tracking-wide text-[#c8e6cf]">도시 귀농 프로젝트</span>
        <p className="relative z-10 max-w-[80%] text-[19px] leading-snug font-extrabold break-words">
          {profile ? `${profile.nickname}님, 오늘도 반가워요!` : "내 프로필"}
        </p>
        <p className="relative z-10 mt-0.5 max-w-[80%] text-[13px] text-white/80">
          도시에서 밭을 키우는 당신을 응원합니다
        </p>
        <span aria-hidden className="absolute -right-1.5 -bottom-3.5 rotate-[-8deg] text-[72px] opacity-20">
          🌱
        </span>
      </div>

      <ReadonlyField label="이름" value={profile?.name} />

      {/* 닉네임 영역 */}
      <div className="flex flex-col gap-2">
        <div className="flex items-center justify-between">
          <span className="text-sm font-semibold">닉네임</span>
          {!isEditing ? (
            <button
              type="button"
              onClick={handleStartEdit}
              className="text-xs font-medium text-primary hover:underline"
            >
              닉네임 수정
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
            {profile?.nickname}
          </div>
        ) : (
          <input
            type="text"
            value={nicknameInput}
            onChange={(e) => setNicknameInput(e.target.value)}
            placeholder="변경할 닉네임을 입력하세요"
            className="w-full rounded-xl bg-surface px-4 py-3.5 text-[15px] outline-none ring-2 ring-primary focus:ring-2"
            autoFocus
          />
        )}
      </div>

      <ReadonlyField label="이메일" value={profile?.email} />

      {/* 비밀번호 변경 버튼 (포인트 스타일 적용) */}
      <button
        type="button"
        onClick={() => setIsPasswordModalOpen(true)}
        className="flex w-full items-center justify-between rounded-xl bg-surface px-4 py-3.5 text-left border border-primary/20 hover:border-primary/50 transition-colors"
      >
        <span className="flex items-center gap-2.5 text-[15px] font-semibold text-primary">
          <LockIcon size={20} className="text-primary" />
          비밀번호 변경
        </span>
        <ChevronRightIcon size={18} className="text-primary/70" />
      </button>

      {/* 메뉴 그룹 영역 */}
      <div className="flex flex-col gap-2">
        <ProfileLinkRow href="/subscription" icon={<TicketIcon size={20} />} label="내 구독" />
        {/* 결제 내역 + 정산 내역 좌우 2분할 카드 */}
        <div className="flex w-full items-center rounded-xl bg-surface">
          {/* 왼쪽: 결제 내역 */}
          <LinkComponent
            href="/profile/payments"
            className="flex flex-1 items-center justify-between px-4 py-3.5 transition-colors hover:bg-surface-strong/50 rounded-l-xl"
          >
            <span className="flex items-center gap-2 text-[15px] font-semibold text-ink">
              <ReceiptIcon size={19} />
              결제 내역
            </span>
            <ChevronRightIcon size={16} className="text-ink-muted" />
          </LinkComponent>
          {/* 세로 구분선 */}
          <div className="h-5 w-[1px] bg-border-subtle/60" />
          {/* 오른쪽: 정산 내역 */}
          <LinkComponent
            href="/host/settlements"
            className="flex flex-1 items-center justify-between px-4 py-3.5 transition-colors hover:bg-surface-strong/50 rounded-r-xl"
          >
            <span className="flex items-center gap-2 text-[15px] font-semibold text-ink">
              <ReceiptIcon size={19} />
              정산 내역
            </span>
            <ChevronRightIcon size={16} className="text-ink-muted" />
          </LinkComponent>
        </div>
        <ProfileLinkRow href="/profile/likes" icon={<HeartIcon size={20} />} label="좋아요한 게시글" />
      </div>

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

      {/* 비밀번호 변경 모달 팝업 */}
      {isPasswordModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 px-4">
          <div className="w-full max-w-sm rounded-2xl bg-surface p-6 shadow-lg">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-base font-bold text-ink">
                {passwordStep === 1 ? "현재 비밀번호 확인" : "새 비밀번호 설정"}
              </h3>
              <button 
                type="button" 
                onClick={closePasswordModal}
                className="text-sm text-ink-muted hover:text-ink"
              >
                ✕
              </button>
            </div>

            {passwordStep === 1 ? (
              <form onSubmit={handleVerifyCurrentPassword} className="flex flex-col gap-4">
                <input
                  type="password"
                  value={currentPasswordInput}
                  onChange={(e) => setCurrentPasswordInput(e.target.value)}
                  placeholder="현재 비밀번호를 입력하세요"
                  className="w-full rounded-xl bg-surface-strong px-4 py-3.5 text-[15px] outline-none ring-2 ring-primary/20 focus:ring-primary"
                  autoFocus
                />
                <button
                  type="submit"
                  disabled={isPasswordLoading}
                  className="h-[48px] w-full rounded-xl bg-primary text-[15px] font-semibold text-white disabled:opacity-50"
                >
                  {isPasswordLoading ? "확인 중..." : "다음"}
                </button>
              </form>
            ) : (
              <form onSubmit={handleChangeNewPassword} className="flex flex-col gap-4">
                <input
                  type="password"
                  value={newPasswordInput}
                  onChange={(e) => setNewPasswordInput(e.target.value)}
                  placeholder="새 비밀번호를 입력하세요"
                  className="w-full rounded-xl bg-surface-strong px-4 py-3.5 text-[15px] outline-none ring-2 ring-primary/20 focus:ring-primary"
                  autoFocus
                />
                <button
                  type="submit"
                  disabled={isPasswordLoading}
                  className="h-[48px] w-full rounded-xl bg-primary text-[15px] font-semibold text-white disabled:opacity-50"
                >
                  {isPasswordLoading ? "변경 중..." : "비밀번호 변경 완료"}
                </button>
              </form>
            )}
          </div>
        </div>
      )}
    </AppShell>
  );
}

function ProfileLinkRow({ href, icon, label }) {
  return (
    <LinkComponent href={href} className="flex items-center justify-between rounded-xl bg-surface px-4 py-3.5">
      <span className="flex items-center gap-2.5 text-[15px] font-semibold text-ink">
        {icon}
        {label}
      </span>
      <ChevronRightIcon size={18} className="text-ink-muted" />
    </LinkComponent>
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