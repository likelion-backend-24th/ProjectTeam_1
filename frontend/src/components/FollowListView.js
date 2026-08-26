"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { AppShell, PageHeader } from "@/components/AppShell";
import { BackIcon } from "@/components/icons";
import { followUser, getFollowerList, getFollowingList, unfollowUser } from "@/lib/api/follow";
import { ApiError } from "@/lib/api/client";
import { useToastStore } from "@/store/toastStore";

export function FollowListView({ mode }) {
  const router = useRouter();
  const showToast = useToastStore((s) => s.showToast);

  const [list, setList] = useState([]);
  // The people I follow, needed even on the followers screen so each row can
  // show whether I already follow that person back.
  const [following, setFollowing] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);

  const followingMap = useMemo(() => new Map(following.map((f) => [f.userId, f.followId])), [following]);

  const load = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      if (mode === "followers") {
        const [followers, followingList] = await Promise.all([getFollowerList(), getFollowingList()]);
        setList(followers);
        setFollowing(followingList);
      } else {
        const followingList = await getFollowingList();
        setList(followingList);
        setFollowing(followingList);
      }
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "불러오지 못했어요.");
    } finally {
      setIsLoading(false);
    }
  }, [mode]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    load();
  }, [load]);

  async function handleFollow(userId) {
    try {
      await followUser(userId);
      const refreshed = await getFollowingList();
      setFollowing(refreshed);
      if (mode === "following") setList(refreshed);
      showToast("팔로우했어요.");
    } catch (err) {
      showToast(err instanceof ApiError ? err.message : "요청에 실패했어요.", "error");
    }
  }

  async function handleUnfollow(followId) {
    try {
      await unfollowUser(followId);
      setFollowing((prev) => prev.filter((f) => f.followId !== followId));
      if (mode === "following") setList((prev) => prev.filter((f) => f.followId !== followId));
      showToast("팔로우를 취소했어요.");
    } catch (err) {
      showToast(err instanceof ApiError ? err.message : "요청에 실패했어요.", "error");
    }
  }

  const title = mode === "followers" ? "팔로워" : "팔로잉";
  const emptyText = mode === "followers" ? "팔로워가 없어요." : "팔로잉한 사용자가 없어요.";

  return (
    <AppShell
      header={
        <PageHeader
          title={title}
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
      {!isLoading && !error && list.length === 0 && (
        <p className="py-10 text-center text-sm text-ink-muted">{emptyText}</p>
      )}
      {!isLoading && !error && list.length > 0 && (
        <ul className="flex flex-col gap-2">
          {list.map((u) => {
            const isFollowing = mode === "following" ? true : followingMap.has(u.userId);
            const followId = mode === "following" ? u.followId : followingMap.get(u.userId);
            return (
              <li key={u.userId} className="flex items-center gap-2.5 rounded-xl bg-surface px-3.5 py-3">
                <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-surface-strong text-sm font-bold text-ink-soft">
                  {u.nickname.slice(0, 1)}
                </div>
                <p className="flex-1 truncate text-[14px] font-semibold">{u.nickname}</p>
                <button
                  type="button"
                  onClick={() => (isFollowing ? handleUnfollow(followId) : handleFollow(u.userId))}
                  className={`shrink-0 rounded-full px-3.5 py-1.5 text-[13px] font-semibold ${
                    isFollowing ? "bg-surface-strong text-ink-soft" : "bg-primary text-white"
                  }`}
                >
                  {isFollowing ? (mode === "following" ? "언팔로우" : "팔로잉") : "팔로우"}
                </button>
              </li>
            );
          })}
        </ul>
      )}
    </AppShell>
  );
}
