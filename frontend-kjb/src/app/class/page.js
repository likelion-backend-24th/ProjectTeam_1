"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { AppShell, PageHeader } from "@/components/AppShell";
import { LocationIcon, PencilIcon, UsersIcon } from "@/components/icons";
import { getClassList } from "@/lib/api/onedayclass";
import { ApiError } from "@/lib/api/client";
import { formatCurrency, formatDateTime } from "@/utils/format";
import { useAuthStore } from "@/store/authStore";

const PAGE_SIZE = 10;
const MAX_PAGE_BUTTONS = 5;

function getPageNumbers(current, totalPages) {
  if (totalPages <= MAX_PAGE_BUTTONS) return Array.from({ length: totalPages }, (_, i) => i);
  let start = Math.max(0, current - Math.floor(MAX_PAGE_BUTTONS / 2));
  let end = start + MAX_PAGE_BUTTONS - 1;
  if (end >= totalPages) {
    end = totalPages - 1;
    start = end - MAX_PAGE_BUTTONS + 1;
  }
  return Array.from({ length: end - start + 1 }, (_, i) => start + i);
}

export default function OneDayClassListPage() {
  const router = useRouter();
  const isHost = useAuthStore((s) => s.isHost);
  const [classes, setClasses] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);

  const loadClasses = useCallback(async (targetPage) => {
    setIsLoading(true);
    setError(null);
    try {
      const res = await getClassList({ page: targetPage, size: PAGE_SIZE });
      const content = res.content.map((cls) => ({
        ...cls,
        description: cls.description ?? "텃밭에서 직접 씨앗을 심고 물을 주며 채소가 자라는 과정을 배우는 체험형 원데이클래스입니다.",
        enrolledCount: cls.enrolledCount ?? 1,
      }));
      setClasses(content);
      setTotalPages(res.totalPages);
      setPage(targetPage);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "클래스 목록을 불러오지 못했어요.");
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    loadClasses(0);
  }, [loadClasses]);

  const pageNumbers = getPageNumbers(page, totalPages);

  return (
    <AppShell header={<PageHeader title="원데이클래스" />}>
      <div className="relative flex flex-col gap-1 overflow-hidden rounded-[18px] bg-gradient-to-br from-[#1a2e22] via-[#2f5138] to-[#3f6b48] px-5 py-[22px] text-white">
        <span className="text-xs font-bold tracking-wide text-[#c8e6cf]">도시 귀농 프로젝트</span>
        <p className="relative z-10 max-w-[80%] text-[19px] leading-snug font-extrabold break-words">
          텃밭 원데이클래스를 둘러보세요
        </p>
        <p className="relative z-10 mt-0.5 max-w-[80%] text-[13px] text-white/80">
          구독 수강권 또는 일반 결제로 바로 신청할 수 있어요
        </p>
        <span aria-hidden className="absolute -right-1.5 -bottom-3.5 rotate-[-8deg] text-[72px] opacity-20">
          🌻
        </span>
      </div>

      {error && <p className="rounded-lg bg-red-50 px-3.5 py-2.5 text-[13px] text-red-700">{error}</p>}

      {!error && classes.length === 0 && !isLoading && (
        <div className="py-15 text-center text-sm text-ink-muted">
          <div className="mx-auto mb-3.5 flex h-14 w-14 items-center justify-center rounded-full bg-surface text-2xl">
            🌱
          </div>
          아직 열린 클래스가 없어요.
        </div>
      )}

      <ul className="flex flex-col gap-3">
        {classes.map((cls) => (
          <li key={cls.id}>
            <Link
              href={`/class/${cls.id}`}
              className="flex flex-col gap-2 rounded-[18px] border border-border border-l-[3px] border-l-class bg-white p-4 active:scale-[0.99]"
            >
              <div className="flex items-center justify-between gap-2">
                <span className="inline-flex items-center rounded-full bg-class-soft px-2.5 py-1 text-xs font-bold whitespace-nowrap text-orange-700">
                  원데이클래스
                </span>
                <span className="shrink-0 text-[15px] font-extrabold text-class">{formatCurrency(cls.price)}</span>
              </div>
              <p className="truncate text-[15px] font-bold">{cls.title}</p>
              {cls.description && <p className="line-clamp-2 text-[13px] text-ink-soft">{cls.description}</p>}
              <div className="flex items-center justify-between gap-2 text-xs text-ink-muted">
                <span>{formatDateTime(cls.date)}</span>
                <div className="flex shrink-0 items-center gap-3">
                  <span className="flex items-center gap-1">
                    <LocationIcon size={14} /> {cls.location}
                  </span>
                  <span className="flex items-center gap-1">
                    <UsersIcon size={14} /> {cls.enrolledCount}/{cls.capacity}명
                  </span>
                </div>
              </div>
            </Link>
          </li>
        ))}
      </ul>

      {isLoading && <p className="py-3 text-center text-sm text-ink-muted">불러오는 중...</p>}

      {!isLoading && classes.length > 0 && totalPages > 1 && (
        <div className="flex items-center justify-center gap-1.5">
          <button
            type="button"
            disabled={page === 0}
            onClick={() => loadClasses(page - 1)}
            aria-label="이전 페이지"
            className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg text-sm font-semibold text-ink-soft disabled:opacity-30"
          >
            ‹
          </button>
          {pageNumbers.map((p) => (
            <button
              key={p}
              type="button"
              onClick={() => loadClasses(p)}
              className={`flex h-8 w-8 shrink-0 items-center justify-center rounded-lg text-sm font-semibold ${
                p === page ? "bg-primary text-white" : "text-ink-soft"
              }`}
            >
              {p + 1}
            </button>
          ))}
          <button
            type="button"
            disabled={page >= totalPages - 1}
            onClick={() => loadClasses(page + 1)}
            aria-label="다음 페이지"
            className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg text-sm font-semibold text-ink-soft disabled:opacity-30"
          >
            ›
          </button>
        </div>
      )}

      {isHost && (
        <button
          type="button"
          onClick={() => router.push("/class/write")}
          aria-label="클래스 등록"
          className="absolute right-5 bottom-[calc(60px_+_20px)] flex h-[52px] w-[52px] items-center justify-center rounded-full bg-class text-white shadow-[0_6px_16px_rgba(0,0,0,0.2)]"
        >
          <PencilIcon size={22} />
        </button>
      )}
    </AppShell>
  );
}