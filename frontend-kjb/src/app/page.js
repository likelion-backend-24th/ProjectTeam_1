"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { AppShell, PageHeader } from "@/components/AppShell";
import { CategoryBadge } from "@/components/CategoryBadge";
import { HeartIcon, PencilIcon, SearchIcon } from "@/components/icons";
import { getBoards } from "@/lib/api/board";
import { ApiError } from "@/lib/api/client";
import { formatRelativeTime } from "@/utils/format";
import { useAuthStore } from "@/store/authStore";

const FILTERS = [
  { label: "전체", value: "ALL" },
  { label: "공지사항", value: "NOTICE" },
  { label: "질문", value: "QNA" },
  { label: "자유게시판", value: "FREE" },
];

const SORT_OPTIONS = [
  { label: "최신순", value: "createdAt,desc" },
  { label: "좋아요순", value: "likeCount,desc" },
];

const CATEGORY_BORDER = {
  NOTICE: "border-l-notice",
  QNA: "border-l-qna",
  FREE: "border-l-free",
};

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

export default function BoardListPage() {
  const router = useRouter();
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);
  const profile = useAuthStore((s) => s.profile);

  const [activeFilter, setActiveFilter] = useState("ALL");
  const [sort, setSort] = useState(SORT_OPTIONS[0].value);
  const [keywordInput, setKeywordInput] = useState("");
  const [keyword, setKeyword] = useState("");

  const [posts, setPosts] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);

  const loadPosts = useCallback(
    async (targetPage) => {
      setIsLoading(true);
      setError(null);
      try {
        const res = await getBoards({
          type: activeFilter === "ALL" ? undefined : activeFilter,
          keyword: keyword || undefined,
          page: targetPage,
          size: PAGE_SIZE,
          sort,
        });
        // Defensive client-side filter: the backend currently returns all
        // categories regardless of the `type` query param, so re-filter here
        // to keep the UI correct even if that server-side bug is present.
        const content =
          activeFilter === "ALL" ? res.content : res.content.filter((p) => p.category === activeFilter);
        setPosts(content);
        setTotalPages(res.totalPages);
        setPage(targetPage);
      } catch (err) {
        setError(err instanceof ApiError ? err.message : "게시글을 불러오지 못했어요.");
      } finally {
        setIsLoading(false);
      }
    },
    [activeFilter, keyword, sort],
  );

  useEffect(() => {
    // Refetch whenever the filter/keyword/sort changes; not derivable from render.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    loadPosts(0);
  }, [loadPosts]);

  function handleSearchSubmit(e) {
    e.preventDefault();
    setKeyword(keywordInput.trim());
  }

  function handleWriteClick() {
    if (!isAuthenticated) {
      router.push("/login?from=/write");
      return;
    }
    router.push("/write");
  }

  const pageNumbers = getPageNumbers(page, totalPages);

  return (
    <AppShell header={<PageHeader title="게시판" />}>
      <div className="relative flex flex-col gap-1 overflow-hidden rounded-[18px] bg-gradient-to-br from-[#1a2e22] via-[#2f5138] to-[#3f6b48] px-5 py-[22px] text-white">
        <span className="text-xs font-bold tracking-wide text-[#c8e6cf]">도시 귀농 프로젝트</span>
        <p className="relative z-10 max-w-[80%] text-[19px] leading-snug font-extrabold break-words">
          {profile ? `${profile.nickName}님, 오늘도 반가워요!` : "이웃들과 텃밭 이야기를 나눠보세요"}
        </p>
        <p className="relative z-10 mt-0.5 max-w-[80%] text-[13px] text-white/80">공지, 질문, 자유 이야기를 한곳에서 확인해요</p>
        <span aria-hidden className="absolute -right-1.5 -bottom-3.5 rotate-[-8deg] text-[72px] opacity-20">
          🌱
        </span>
      </div>

      <form className="flex items-center gap-2 rounded-xl bg-surface px-3.5 py-3 text-ink-muted" onSubmit={handleSearchSubmit}>
        <SearchIcon />
        <input
          type="text"
          placeholder="제목, 작성자 검색..."
          value={keywordInput}
          onChange={(e) => setKeywordInput(e.target.value)}
          className="flex-1 bg-transparent text-sm text-ink outline-none"
        />
      </form>

      <div className="flex items-center justify-between gap-2">
        <div className="flex flex-wrap items-center gap-2">
          {FILTERS.map((f) => (
            <button
              key={f.value}
              type="button"
              onClick={() => setActiveFilter(f.value)}
              className={`shrink-0 rounded-full px-3.5 py-2 text-[13px] font-semibold ${
                activeFilter === f.value ? "bg-primary text-white" : "bg-surface text-ink-soft"
              }`}
            >
              {f.label}
            </button>
          ))}
        </div>

        <select
          value={sort}
          onChange={(e) => setSort(e.target.value)}
          className="shrink-0 rounded-full border border-border bg-white px-3 py-2 text-[13px] font-semibold text-ink-soft outline-none"
        >
          {SORT_OPTIONS.map((s) => (
            <option key={s.value} value={s.value}>
              {s.label}
            </option>
          ))}
        </select>
      </div>

      {error && <p className="rounded-lg bg-red-50 px-3.5 py-2.5 text-[13px] text-red-700">{error}</p>}

      {!error && posts.length === 0 && !isLoading && (
        <div className="py-15 text-center text-sm text-ink-muted">
          <div className="mx-auto mb-3.5 flex h-14 w-14 items-center justify-center rounded-full bg-surface text-2xl">
            🌾
          </div>
          아직 게시글이 없어요.
          <br />
          첫 게시글을 작성해보세요!
        </div>
      )}

      <ul className="flex flex-col gap-3">
        {posts.map((post) => (
          <li key={post.id}>
            <Link
              href={`/board/${post.id}`}
              className={`flex flex-col gap-2 rounded-[18px] border border-border border-l-[3px] bg-white p-4 active:scale-[0.99] ${CATEGORY_BORDER[post.category]}`}
            >
              <div className="flex items-center justify-between">
                <CategoryBadge category={post.category} />
                <span className="text-xs text-ink-muted">조회 {post.viewCount}</span>
              </div>
              <p className="truncate text-[15px] font-bold">{post.title}</p>
              <p className="truncate text-[13px] text-ink-soft">{post.content}</p>
              <div className="flex items-center gap-3.5 text-xs text-ink-muted">
                <span className="flex items-center gap-2">
                  <span className="flex h-[22px] w-[22px] items-center justify-center rounded-full bg-surface-strong text-[11px] font-bold text-ink-soft">
                    {post.writer.slice(0, 1)}
                  </span>
                  {post.writer}
                </span>
                <span>{formatRelativeTime(post.createdAt)}</span>
                <span className="flex items-center gap-1">
                  <HeartIcon size={14} /> {post.likeCount}
                </span>
              </div>
            </Link>
          </li>
        ))}
      </ul>

      {isLoading && <p className="py-3 text-center text-sm text-ink-muted">불러오는 중...</p>}

      {!isLoading && posts.length > 0 && totalPages > 1 && (
        <div className="flex items-center justify-center gap-1.5">
          <button
            type="button"
            disabled={page === 0}
            onClick={() => loadPosts(page - 1)}
            aria-label="이전 페이지"
            className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg text-sm font-semibold text-ink-soft disabled:opacity-30"
          >
            ‹
          </button>
          {pageNumbers.map((p) => (
            <button
              key={p}
              type="button"
              onClick={() => loadPosts(p)}
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
            onClick={() => loadPosts(page + 1)}
            aria-label="다음 페이지"
            className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg text-sm font-semibold text-ink-soft disabled:opacity-30"
          >
            ›
          </button>
        </div>
      )}

      <button
        type="button"
        onClick={handleWriteClick}
        aria-label="글쓰기"
        className="absolute right-5 bottom-[calc(60px_+_20px)] flex h-[52px] w-[52px] items-center justify-center rounded-full bg-primary text-white shadow-[0_6px_16px_rgba(0,0,0,0.2)]"
      >
        <PencilIcon size={22} />
      </button>
    </AppShell>
  );
}
