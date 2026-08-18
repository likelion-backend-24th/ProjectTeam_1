"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { HomeIcon, PersonIcon, ShieldIcon } from "./icons";
import { useAuthStore } from "@/store/authStore";

export function AppShell({ header, children, showNav = true, noPadding = false }) {
  const pathname = usePathname();
  const isAdmin = useAuthStore((s) => s.isAdmin);

  return (
    <div className="relative flex h-svh w-full max-w-[480px] flex-col overflow-hidden bg-white shadow-[0_0_40px_rgba(0,0,0,0.06)]">
      {header}
      {/*
        [&>*]:shrink-0 stops flexbox from crushing this scroll container's
        direct children (e.g. the home banner) once total content height
        exceeds the viewport — without it, flex-shrink's default of 1 lets
        them get squashed instead of the container scrolling past them.
      */}
      <main
        className={`flex flex-1 flex-col gap-4 overflow-y-auto [&>*]:shrink-0 ${noPadding ? "" : "px-4 pt-5 pb-6"}`}
      >
        {children}
      </main>
      {showNav && (
        <nav className="flex h-[60px] shrink-0 items-center justify-around border-t border-border bg-white">
          <Link
            href="/"
            className={`flex h-full flex-1 flex-col items-center justify-center gap-0.5 ${
              pathname === "/" ? "text-ink" : "text-ink-muted"
            }`}
          >
            <HomeIcon size={22} />
            <span className="text-[11px]">홈</span>
          </Link>
          <Link
            href="/profile"
            className={`flex h-full flex-1 flex-col items-center justify-center gap-0.5 ${
              pathname === "/profile" ? "text-ink" : "text-ink-muted"
            }`}
          >
            <PersonIcon size={22} />
            <span className="text-[11px]">내 프로필</span>
          </Link>
          {isAdmin && (
            <Link
              href="/admin"
              className={`flex h-full flex-1 flex-col items-center justify-center gap-0.5 ${
                pathname === "/admin" ? "text-ink" : "text-ink-muted"
              }`}
            >
              <ShieldIcon size={22} />
              <span className="text-[11px]">관리자</span>
            </Link>
          )}
        </nav>
      )}
    </div>
  );
}

export function PageHeader({ title, left, right }) {
  return (
    <header className="flex shrink-0 items-center justify-between gap-2 border-b border-border bg-white px-4 py-3.5">
      <div className="flex min-w-8 shrink-0">{left}</div>
      <h1 className="flex-1 text-center text-[17px] font-bold">{title}</h1>
      <div className="flex min-w-8 shrink-0 justify-end whitespace-nowrap">{right}</div>
    </header>
  );
}
