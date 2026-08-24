"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { AppShell, PageHeader } from "@/components/AppShell";
import { CategoryBadge } from "@/components/CategoryBadge";
import { CommentForm } from "@/components/CommentForm";
import { ReplyItem } from "@/components/ReplyItem";
import { BackIcon, HeartIcon } from "@/components/icons";
import { deleteBoard, getBoard, likeBoard, unlikeBoard } from "@/lib/api/board";
import { createBoardComment, deleteBoardComment, getBoardComments } from "@/lib/api/comments";
import { createReply, deleteReply, getReplies } from "@/lib/api/reply";
import { ApiError } from "@/lib/api/client";
import { CATEGORY_LABEL, formatRelativeTime } from "@/utils/format";
import { useAuthStore } from "@/store/authStore";
import { useToastStore } from "@/store/toastStore";
import { isBoardLiked, setBoardLiked } from "@/lib/likedBoards";

export default function BoardDetailPage() {
  const { boardId } = useParams();
  const id = Number(boardId);
  const router = useRouter();
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);
  const profile = useAuthStore((s) => s.profile);
  const isAdmin = useAuthStore((s) => s.isAdmin);
  const showToast = useToastStore((s) => s.showToast);

  const [post, setPost] = useState(null);
  const [liked, setLiked] = useState(false);
  const [comments, setComments] = useState([]);
  const [replies, setReplies] = useState([]);
  const [replyDraft, setReplyDraft] = useState("");
  const [isReplySubmitting, setIsReplySubmitting] = useState(false);
  const [showReplyForm, setShowReplyForm] = useState(false);
  const [showComments, setShowComments] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);

  const load = useCallback(
    async (signal) => {
      setIsLoading(true);
      setError(null);
      try {
        const [boardData, commentData, replyData] = await Promise.all([
          getBoard(id, signal),
          getBoardComments(id),
          getReplies(id),
        ]);
        if (signal?.aborted) return;
        setPost(boardData);
        setComments(commentData);
        setReplies(replyData);
      } catch (err) {
        if (signal?.aborted) return;
        setError(err instanceof ApiError ? err.message : "게시글을 불러오지 못했어요.");
      } finally {
        if (!signal?.aborted) setIsLoading(false);
      }
    },
    [id],
  );

  useEffect(() => {
    if (!Number.isFinite(id)) return;
    // getBoard() increments the server-side view count as a side effect of the
    // GET, so this abort guard prevents React StrictMode's dev-only
    // double-invoke from inflating the view count.
    const controller = new AbortController();
    // eslint-disable-next-line react-hooks/set-state-in-effect
    load(controller.signal);
    return () => controller.abort();
  }, [id, load]);

  useEffect(() => {
    // The board detail API doesn't report whether the current user already
    // liked the post, so restore it from our local per-account record
    // (localStorage), which the API can't provide synchronously during render.
    if (profile?.email) {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setLiked(isBoardLiked(profile.email, id));
    }
  }, [profile?.email, id]);

  function requireLogin() {
    router.push(`/login?from=${encodeURIComponent(`/board/${id}`)}`);
  }

  async function handleLikeToggle() {
    if (!isAuthenticated) return requireLogin();
    try {
      const res = liked ? await unlikeBoard(id) : await likeBoard(id);
      setLiked(res.liked);
      setBoardLiked(profile?.email, id, res.liked);
      setPost((prev) => (prev ? { ...prev, likeCount: res.likeCount } : prev));
    } catch (err) {
      showToast(err instanceof ApiError ? err.message : "요청에 실패했어요.", "error");
    }
  }

  async function handleDeletePost() {
    if (!window.confirm("게시글을 삭제할까요?")) return;
    try {
      await deleteBoard(id);
      showToast("게시글이 삭제되었어요.");
      router.push("/");
    } catch (err) {
      showToast(err instanceof ApiError ? err.message : "삭제에 실패했어요.", "error");
    }
  }

  async function handleAddComment(content) {
    if (!isAuthenticated) return requireLogin();
    try {
      const created = await createBoardComment(id, { content });
      setComments((prev) => [...prev, created]);
      showToast("댓글이 등록되었어요.");
    } catch (err) {
      showToast(err instanceof ApiError ? err.message : "댓글 등록에 실패했어요.", "error");
      throw err; // let the comment form know it failed so it keeps the draft text
    }
  }

  async function handleDeleteComment(commentId) {
    if (!window.confirm("댓글을 삭제할까요?")) return;
    try {
      await deleteBoardComment(commentId);
      setComments((prev) => prev.filter((c) => c.id !== commentId));
      showToast("댓글이 삭제되었어요.");
    } catch (err) {
      showToast(err instanceof ApiError ? err.message : "댓글 삭제에 실패했어요.", "error");
    }
  }

  async function handleAddReply(e) {
    e.preventDefault();
    if (!isAuthenticated) return requireLogin();
    const trimmed = replyDraft.trim();
    if (!trimmed) return;

    setIsReplySubmitting(true);
    try {
      const created = await createReply(id, { content: trimmed });
      setReplies((prev) => [...prev, created]);
      setReplyDraft("");
      setShowReplyForm(false);
      showToast("답글이 등록되었어요.");
    } catch (err) {
      showToast(err instanceof ApiError ? err.message : "답글 등록에 실패했어요.", "error");
    } finally {
      setIsReplySubmitting(false);
    }
  }

  async function handleDeleteReply(replyId) {
    if (!window.confirm("답글을 삭제할까요?")) return;
    try {
      await deleteReply(replyId);
      setReplies((prev) => prev.filter((r) => r.id !== replyId));
      showToast("답글이 삭제되었어요.");
    } catch (err) {
      showToast(err instanceof ApiError ? err.message : "답글 삭제에 실패했어요.", "error");
    }
  }

  const isOwner = !!profile && !!post && profile.nickname === post.writer;
  const canManagePost = isOwner || isAdmin;

  return (
    <AppShell
      noPadding
      header={
        <PageHeader
          title={post ? CATEGORY_LABEL[post.category] : "게시글"}
          left={
            <button className="flex h-8 w-8 items-center justify-center rounded-lg" onClick={() => router.back()} aria-label="뒤로가기">
              <BackIcon />
            </button>
          }
          right={
            canManagePost ? (
              <div className="flex gap-2">
                <Link href={`/board/${id}/edit`} className="text-[13px] font-medium text-ink-soft">
                  수정
                </Link>
                <button className="text-[13px] font-medium text-red-500" onClick={handleDeletePost}>
                  삭제
                </button>
              </div>
            ) : undefined
          }
        />
      }
    >
      <div className="flex flex-1 flex-col gap-4 px-4 pt-4">
        {isLoading && <p className="text-center text-sm text-ink-muted">불러오는 중...</p>}
        {error && <p className="rounded-lg bg-red-50 px-3.5 py-2.5 text-[13px] text-red-700">{error}</p>}

        {post && (
          <>
            <div className="flex items-center justify-between">
              <CategoryBadge category={post.category} />
              <span className="text-xs text-ink-muted">조회 {post.viewCount}</span>
            </div>

            <h2 className="text-xl leading-snug font-extrabold">{post.title}</h2>

            <div className="flex items-center gap-2.5">
              <div className="flex h-[38px] w-[38px] shrink-0 items-center justify-center rounded-full bg-surface-strong text-sm font-bold text-ink-soft">
                {post.writer.slice(0, 1)}
              </div>
              <div>
                <p className="text-sm font-bold">{post.writer}</p>
                <p className="text-xs text-ink-muted">{formatRelativeTime(post.createdAt)}</p>
              </div>
            </div>

            <p className="text-[15px] leading-relaxed break-words whitespace-pre-wrap">{post.content}</p>

            <div className="flex items-center gap-4 border-t border-border pt-2">
              <button
                type="button"
                className={`flex items-center gap-1.5 text-[13px] font-semibold ${liked ? "text-red-500" : "text-ink-soft"}`}
                onClick={handleLikeToggle}
              >
                <HeartIcon size={18} filled={liked} /> {post.likeCount}
              </button>
              <span className="text-[13px] font-semibold text-ink-soft">댓글 {comments.length}</span>
            </div>

            <hr className="border-border" />

            <div className="flex flex-col gap-2">
              <div className="flex items-center justify-between">
                <p className="flex items-center gap-1.5 text-[15px] font-extrabold">
                  댓글 <span className="text-accent">{comments.length}</span>
                </p>
                <button
                  type="button"
                  className="text-[13px] font-bold text-ink-soft"
                  onClick={() => setShowComments((v) => !v)}
                >
                  {showComments ? "숨기기" : "보기"}
                </button>
              </div>

              {showComments && (
                <ul>
                  {comments.length === 0 && <li className="py-6 text-center text-sm text-ink-muted">첫 댓글을 작성해주세요</li>}
                  {comments.map((c) => (
                    <li key={c.id} className="flex gap-2.5 border-b border-border py-3.5 last:border-none">
                      <div className="flex h-[38px] w-[38px] shrink-0 items-center justify-center rounded-full bg-surface-strong text-sm font-bold text-ink-soft">
                        {c.nickname.slice(0, 1)}
                      </div>
                      <div className="flex flex-1 flex-col gap-1">
                        <div className="flex items-center justify-between">
                          <div className="flex items-baseline gap-2">
                            <span className="text-[13px] font-bold">{c.nickname}</span>
                            <span className="text-xs text-ink-muted">{formatRelativeTime(c.createdAt)}</span>
                          </div>
                          {profile?.nickname === c.nickname && (
                            <button type="button" className="text-xs text-ink-muted" onClick={() => handleDeleteComment(c.id)}>
                              삭제
                            </button>
                          )}
                        </div>
                        <p className="text-sm leading-relaxed break-words whitespace-pre-wrap">{c.content}</p>
                      </div>
                    </li>
                  ))}
                </ul>
              )}
            </div>

            <hr className="border-border" />

            <div className="flex flex-col gap-3">
              <div className="flex items-center justify-between">
                <p className="flex items-center gap-1.5 text-[15px] font-extrabold">
                  답글 <span className="text-accent">{replies.length}</span>
                </p>
                {!isOwner && (
                  <button
                    type="button"
                    className="text-[13px] font-bold text-ink-soft"
                    onClick={() => (isAuthenticated ? setShowReplyForm((v) => !v) : requireLogin())}
                  >
                    답글 작성
                  </button>
                )}
              </div>

              {showReplyForm && (
                <form onSubmit={handleAddReply} className="flex flex-col gap-2">
                  <textarea
                    placeholder="내용을 입력하세요..."
                    value={replyDraft}
                    onChange={(e) => setReplyDraft(e.target.value)}
                    className="min-h-[90px] w-full rounded-xl bg-surface p-3.5 text-sm outline-none"
                  />
                  <div className="flex gap-2">
                    <button
                      type="submit"
                      disabled={!replyDraft.trim() || isReplySubmitting}
                      className="h-[50px] flex-1 rounded-xl bg-primary text-[15px] font-semibold text-white disabled:opacity-50"
                    >
                      등록
                    </button>
                    <button
                      type="button"
                      onClick={() => {
                        setShowReplyForm(false);
                        setReplyDraft("");
                      }}
                      className="h-[50px] flex-1 rounded-xl bg-surface text-[15px] font-semibold text-ink"
                    >
                      취소
                    </button>
                  </div>
                </form>
              )}

              {replies.length === 0 && !showReplyForm && (
                <p className="py-6 text-center text-sm text-ink-muted">첫 답글을 작성해주세요</p>
              )}

              {replies.map((r) => (
                <ReplyItem
                  key={r.id}
                  reply={r}
                  currentNickname={profile?.nickname}
                  isAuthenticated={isAuthenticated}
                  onDeleteReply={handleDeleteReply}
                  onRequireLogin={requireLogin}
                />
              ))}
            </div>
          </>
        )}
      </div>

      {post && <CommentForm onSubmit={handleAddComment} />}
    </AppShell>
  );
}
