"use client";

import { useEffect, useState } from "react";
import { AppShell, PageHeader } from "@/components/AppShell";
import { RequireAuth } from "@/components/RequireAuth";
import { PostList } from "@/components/PostList";
import { getFeed } from "@/lib/api/feed";
import { ApiError } from "@/lib/api/client";
import { useAuthStore } from "@/store/authStore";

function FeedView() {
  const profile = useAuthStore((s) => s.profile);
  const [posts, setPosts] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    let cancelled = false;
    getFeed({ page: 0, size: 20 })
      .then((res) => {
        if (!cancelled) setPosts(res.content);
      })
      .catch((err) => {
        if (!cancelled) setError(err instanceof ApiError ? err.message : "불러오지 못했어요.");
      })
      .finally(() => {
        if (!cancelled) setIsLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <AppShell header={<PageHeader title="피드" />}>
      <div className="relative flex flex-col gap-1 overflow-hidden rounded-[18px] bg-gradient-to-br from-[#1a2e22] via-[#2f5138] to-[#3f6b48] px-5 py-[22px] text-white">
        <span className="text-xs font-bold tracking-wide text-[#c8e6cf]">도시 귀농 프로젝트</span>
        <p className="relative z-10 max-w-[80%] text-[19px] leading-snug font-extrabold break-words">
          {profile ? `${profile.nickname}님, 오늘도 반가워요!` : "팔로우한 이웃들의 소식을 확인해요"}
        </p>
        <p className="relative z-10 mt-0.5 max-w-[80%] text-[13px] text-white/80">
          팔로우한 사용자의 새 게시글을 한곳에서 모아봐요
        </p>
        <span aria-hidden className="absolute -right-1.5 -bottom-3.5 rotate-[-8deg] text-[72px] opacity-20">
          🌱
        </span>
      </div>

      {isLoading && <p className="py-10 text-center text-sm text-ink-muted">불러오는 중...</p>}
      {error && <p className="rounded-lg bg-red-50 px-3.5 py-2.5 text-[13px] text-red-700">{error}</p>}
      {!isLoading && !error && <PostList posts={posts} emptyText="팔로우한 사용자의 게시글이 없어요." />}
    </AppShell>
  );
}

export default function FeedPage() {
  return (
    <RequireAuth>
      <FeedView />
    </RequireAuth>
  );
}
