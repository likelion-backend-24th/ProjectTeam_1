"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { AppShell, PageHeader } from "@/components/AppShell";
import { getFarms } from "@/lib/api/farm";
import { API_BASE_URL, ApiError } from "@/lib/api/client";
import { formatCurrency, formatDate } from "@/utils/format";

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

export default function FarmListPage() {
  const [keywordInput, setKeywordInput] = useState("");
  const [farms, setFarms] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);

  const loadFarms = useCallback(async (targetPage) => {
    setIsLoading(true);
    setError(null);
    try {
      const res = await getFarms({ page: targetPage, size: PAGE_SIZE });
      setFarms(res.content);
      setTotalPages(res.totalPages);
      setPage(targetPage);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "밭 목록을 불러오지 못했어요.");
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    loadFarms(0);
  }, [loadFarms]);

  const pageNumbers = getPageNumbers(page, totalPages);

  return (
    <AppShell header={<PageHeader title="땅 매입" />}>
      <div className="relative flex flex-col gap-1 overflow-hidden rounded-[18px] bg-gradient-to-br from-[#1a2e22] via-[#2f5138] to-[#3f6b48] px-5 py-[22px] text-white">
        <span className="text-xs font-bold tracking-wide text-[#c8e6cf]">도시 귀농 프로젝트</span>
        <p className="relative z-10 max-w-[80%] text-[19px] leading-snug font-extrabold break-words">
          나만의 텃밭이 될 땅을 둘러보세요
        </p>
        <p className="relative z-10 mt-0.5 max-w-[80%] text-[13px] text-white/80">
          마음에 드는 땅을 임대하거나 매입해보세요
        </p>
        <span aria-hidden className="absolute -right-1.5 -bottom-3.5 rotate-[-8deg] text-[72px] opacity-20">
          🌻
        </span>
      </div>

      {error && <p className="rounded-lg bg-red-50 px-3.5 py-2.5 text-[13px] text-red-700">{error}</p>}

      {!error && farms.length === 0 && !isLoading && (
        <div className="py-15 text-center text-sm text-ink-muted">
          <div className="mx-auto mb-3.5 flex h-14 w-14 items-center justify-center rounded-full bg-surface text-2xl">
            🌱
          </div>
          등록된 밭이 없어요.
        </div>
      )}

      <ul className="flex flex-col gap-3">
        {farms.map((farm) => (
          <li key={farm.id}>
            <Link
              href={`/farms/${farm.id}`}
              className="flex gap-3 rounded-2xl border border-border bg-white p-3 active:scale-[0.99]"
            >
              {farm.thumbnailUrl ? (
                <img
                  src={`${API_BASE_URL}${farm.thumbnailUrl}`}
                  alt={farm.title}
                  className="h-20 w-20 shrink-0 rounded-xl object-cover"
                />
              ) : (
                <div className="h-20 w-20 shrink-0 rounded-xl bg-gradient-to-br from-emerald-200 to-emerald-100" />
              )}
              <div className="flex flex-1 flex-col justify-center gap-1 overflow-hidden">
                <p className="truncate text-[15px] font-bold">{farm.title}</p>
                <p className="truncate text-[13px] text-ink-muted">{farm.location}</p>
                <p className="text-[13px] text-ink-muted">{farm.area}m²</p>
                <p className="truncate text-[12px] text-ink-muted">
                  {farm.ownerNickname} · {formatDate(farm.createdAt)}
                </p>
                <p className="text-[14px] font-bold text-ink">월 {formatCurrency(farm.monthlyRent)}</p>
              </div>
            </Link>
          </li>
        ))}
      </ul>

      {isLoading && <p className="py-3 text-center text-sm text-ink-muted">불러오는 중...</p>}

      {!isLoading && farms.length > 0 && totalPages > 1 && (
        <div className="flex items-center justify-center gap-1.5">
          <button
            type="button"
            disabled={page === 0}
            onClick={() => loadFarms(page - 1)}
            aria-label="이전 페이지"
            className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg text-sm font-semibold text-ink-soft disabled:opacity-30"
          >
            ‹
          </button>
          {pageNumbers.map((p) => (
            <button
              key={p}
              type="button"
              onClick={() => loadFarms(p)}
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
            onClick={() => loadFarms(page + 1)}
            aria-label="다음 페이지"
            className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg text-sm font-semibold text-ink-soft disabled:opacity-30"
          >
            ›
          </button>
        </div>
      )}
    </AppShell>
  );
}