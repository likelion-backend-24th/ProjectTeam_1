"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { AppShell, PageHeader } from "@/components/AppShell";
import { RequireAuth } from "@/components/RequireAuth";
import { BackIcon } from "@/components/icons";
import { PostList } from "@/components/PostList";
import { getLikedBoards } from "@/lib/api/profile";
import { ApiError } from "@/lib/api/client";

function LikedBoardsView() {
  const router = useRouter();
  const [posts, setPosts] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    let cancelled = false;
    getLikedBoards({ page: 0, size: 20 })
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
    <AppShell
      header={
        <PageHeader
          title="좋아요한 게시글"
          left={
            <button
              className="flex h-8 w-8 items-center justify-center rounded-lg"
              onClick={() => router.back()}
              aria-label="뒤로가기"
            >
              <BackIcon />
            </button>
          }
        />
      }
    >
      {isLoading && <p className="py-10 text-center text-sm text-ink-muted">불러오는 중...</p>}
      {error && <p className="rounded-lg bg-red-50 px-3.5 py-2.5 text-[13px] text-red-700">{error}</p>}
      {!isLoading && !error && <PostList posts={posts} emptyText="좋아요한 게시글이 없어요." />}
    </AppShell>
  );
}

export default function ProfileLikesPage() {
  return (
    <RequireAuth>
      <LikedBoardsView />
    </RequireAuth>
  );
}
