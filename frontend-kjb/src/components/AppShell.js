"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { HomeIcon, FeedIcon, BookIcon, SproutIcon, PersonIcon, ShieldIcon } from "./icons";
import { useAuthStore } from "@/store/authStore";

const NAV_ITEMS = [
  { href: "/", label: "홈", icon: HomeIcon, match: (p) => p === "/" },
  { href: "/feed", label: "피드", icon: FeedIcon, match: (p) => p.startsWith("/feed") },
  { href: "/class", label: "클래스", icon: BookIcon, match: (p) => p.startsWith("/class") },
  { href: "/farms", label: "땅 매입", icon: SproutIcon, match: (p) => p.startsWith("/farms") },
  { href: "/mypage", label: "마이", icon: PersonIcon, match: (p) => p.startsWith("/mypage") },
];

export function AppShell({ header, children, showNav = true, noPadding = false }) {
  const pathname = usePathname();
  const isAdmin = useAuthStore((s) => s.isAdmin);

  return (
    <div className="relative flex h-svh w-full max-w-[480px] flex-col overflow-hidden bg-white shadow-[0_0_40px_rgba(0,0,0,0.06)]">
      {header}
      <main
        className={`flex flex-1 flex-col gap-4 overflow-y-auto [&>*]:shrink-0 ${noPadding ? "" : "px-4 pt-5 pb-6"}`}
      >
        {children}
      </main>
      {showNav && (
        <nav className="flex h-[60px] shrink-0 items-center justify-around border-t border-border bg-white">
          {NAV_ITEMS.map(({ href, label, icon: Icon, match }) => (
            <Link
              key={href}
              href={href}
              className={`flex h-full flex-1 flex-col items-center justify-center gap-0.5 ${
                match(pathname) ? "text-ink" : "text-ink-muted"
              }`}
            >
              <Icon size={22} />
              <span className="text-[11px]">{label}</span>
            </Link>
          ))}
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