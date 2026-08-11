"use client";

import { useRouter } from "next/navigation";
import { AppShell, PageHeader } from "@/components/AppShell";
import { RequireAuth } from "@/components/RequireAuth";
import { useAuthStore } from "@/store/authStore";

function ProfileView() {
  const profile = useAuthStore((s) => s.profile);
  const logout = useAuthStore((s) => s.logout);
  const router = useRouter();

  async function handleLogout() {
    await logout();
    router.replace("/login");
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

      <button
        type="button"
        onClick={handleLogout}
        className="mt-2 h-[50px] w-full rounded-xl bg-surface text-[15px] font-semibold text-ink"
      >
        로그아웃
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
