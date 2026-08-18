"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { AppShell, PageHeader } from "@/components/AppShell";
import { RequireAuth } from "@/components/RequireAuth";
import { ChevronRightIcon, ReceiptIcon, TicketIcon } from "@/components/icons";
import { useAuthStore } from "@/store/authStore";
import { useToastStore } from "@/store/toastStore";
import { ApiError } from "@/lib/api/client";

function ProfileView() {
  const profile = useAuthStore((s) => s.profile);
  const logout = useAuthStore((s) => s.logout);
  const withdraw = useAuthStore((s) => s.withdraw);
  const showToast = useToastStore((s) => s.showToast);
  const router = useRouter();

  const [isWithdrawing, setIsWithdrawing] = useState(false);

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

  return (
    <AppShell header={<PageHeader title="내 프로필" />}>
      <div className="flex justify-center pt-3 pb-1">
        <div className="flex h-[76px] w-[76px] items-center justify-center rounded-full bg-surface-strong text-[28px] font-bold text-ink-soft">
          {profile?.name?.slice(0, 1) ?? "?"}
        </div>
      </div>

      <ReadonlyField label="이름" value={profile?.name} />
      <ReadonlyField label="닉네임" value={profile?.nickName} />
      <ReadonlyField label="이메일" value={profile?.email} />

      <div className="flex flex-col gap-2">
        <ProfileLinkRow href="/subscription" icon={<TicketIcon size={20} />} label="내 구독" />
        <ProfileLinkRow href="/profile/payments" icon={<ReceiptIcon size={20} />} label="결제 내역" />
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
    </AppShell>
  );
}

function ProfileLinkRow({ href, icon, label }) {
  return (
    <Link href={href} className="flex items-center justify-between rounded-xl bg-surface px-4 py-3.5">
      <span className="flex items-center gap-2.5 text-[15px] font-semibold text-ink">
        {icon}
        {label}
      </span>
      <ChevronRightIcon size={18} className="text-ink-muted" />
    </Link>
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
