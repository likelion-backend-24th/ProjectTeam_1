"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { AppShell, PageHeader } from "@/components/AppShell";
import { HeartIcon, PencilIcon, SearchIcon } from "@/components/icons";
import { getFarms } from "@/lib/api/farm";
import { API_BASE_URL, ApiError } from "@/lib/api/client";
import { formatCurrency } from "@/utils/format";
import { useAuthStore } from "@/store/authStore";

const SORT_OPTIONS = [
  { label: "전체", value: undefined },
  { label: "최신순", value: "createdAt,desc" },
];

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
  const router = useRouter();
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);

  const [activeSort, setActiveSort] = useState(SORT_OPTIONS[0].value);
  const [keywordInput, setKeywordInput] = useState("");
  const [farms, setFarms] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);

  const loadFarms = useCallback(
    async (targetPage) => {
      setIsLoading(true);
      setError(null);
      try {
        const res = await getFarms({ page: targetPage, size: PAGE_SIZE, sort: activeSort });
        setFarms(res.content);
        setTotalPages(res.totalPages);
        setPage(targetPage);
      } catch (err) {
        setError(err instanceof ApiError ? err.message : "밭 목록을 불러오지 못했어요.");
      } finally {
        setIsLoading(false);
      }
    },
    [activeSort],
  );

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    loadFarms(0);
  }, [loadFarms]);

  function handleSearchSubmit(e) {
    e.preventDefault();
    // TODO: 백엔드 GET /api/farms에 검색(keyword) 파라미터가 아직 없어서 지금은 UI만 있고 동작 안 함
  }

  function handleRegisterClick() {
    if (!isAuthenticated) {
      router.push("/login?from=/farms/new");
      return;
    }
    router.push("/farms/new");
  }

  const pageNumbers = getPageNumbers(page, totalPages);

  return (
    <AppShell header={<PageHeader title="땅 매입" />}>
      <form className="flex items-center gap-2 rounded-xl bg-surface px-3.5 py-3 text-ink-muted" onSubmit={handleSearchSubmit}>
        <SearchIcon />
        <input
          type="text"
          placeholder="제목, 지역 검색..."
          value={keywordInput}
          onChange={(e) => setKeywordInput(e.target.value)}
          className="flex-1 bg-transparent text-sm text-ink outline-none"
        />
      </form>

      <div className="flex items-center gap-2">
        {SORT_OPTIONS.map((s) => (
          <button
            key={s.label}
            type="button"
            onClick={() => setActiveSort(s.value)}
            className={`shrink-0 rounded-full px-3.5 py-2 text-[13px] font-semibold ${
              activeSort === s.value ? "bg-primary text-white" : "bg-surface text-ink-soft"
            }`}
          >
            {s.label}
          </button>
        ))}
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
                <div className="flex items-center justify-between gap-2">
                  <p className="truncate text-[15px] font-bold">{farm.title}</p>
                  <HeartIcon size={16} className="shrink-0 text-ink-muted" />
                </div>
                <p className="truncate text-[13px] text-ink-muted">{farm.location}</p>
                <p className="text-[13px] text-ink-muted">{farm.area}m²</p>
                <p className="text-[14px] font-bold text-ink">월 {formatCurrency(farm.monthlyRent)}원</p>
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

      <button
        type="button"
        onClick={handleRegisterClick}
        aria-label="밭 등록"
        className="absolute right-5 bottom-[calc(60px_+_20px)] flex h-[52px] w-[52px] items-center justify-center rounded-full bg-primary text-white shadow-[0_6px_16px_rgba(0,0,0,0.2)]"
      >
        <PencilIcon size={22} />
      </button>
    </AppShell>
  );
}