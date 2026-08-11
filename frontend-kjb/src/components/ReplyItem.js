"use client";

import { useState } from "react";
import { createReplyComment, deleteReplyComment, getReplyComments } from "@/lib/api/reply";
import { ApiError } from "@/lib/api/client";
import { formatRelativeTime } from "@/utils/format";

export function ReplyItem({ reply, currentNickname, isAuthenticated, onDeleteReply, onRequireLogin }) {
  const [isOpen, setIsOpen] = useState(false);
  const [hasLoaded, setHasLoaded] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [comments, setComments] = useState([]);
  const [draft, setDraft] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState(null);

  async function toggleOpen() {
    if (!isOpen && !isAuthenticated) {
      onRequireLogin();
      return;
    }
    const next = !isOpen;
    setIsOpen(next);
    if (next && !hasLoaded) {
      setIsLoading(true);
      setError(null);
      try {
        const data = await getReplyComments(reply.id);
        setComments(data);
        setHasLoaded(true);
      } catch (err) {
        setError(err instanceof ApiError ? err.message : "댓글을 불러오지 못했어요.");
      } finally {
        setIsLoading(false);
      }
    }
  }

  async function handleAddComment(e) {
    e.preventDefault();
    if (!isAuthenticated) return onRequireLogin();
    const trimmed = draft.trim();
    if (!trimmed) return;

    setIsSubmitting(true);
    setError(null);
    try {
      const created = await createReplyComment(reply.id, { content: trimmed });
      setComments((prev) => [...prev, created]);
      setDraft("");
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "댓글 등록에 실패했어요.");
    } finally {
      setIsSubmitting(false);
    }
  }

  async function handleDeleteComment(commentId) {
    if (!window.confirm("댓글을 삭제할까요?")) return;
    await deleteReplyComment(commentId);
    setComments((prev) => prev.filter((c) => c.id !== commentId));
  }

  return (
    <div
      className={`flex flex-col gap-2 rounded-xl border p-3.5 ${
        reply.isAdopted ? "border-accent bg-accent-soft" : "border-border"
      }`}
    >
      {reply.isAdopted && (
        <span className="w-fit rounded-full border border-accent bg-white px-2 py-0.5 text-[11px] font-extrabold text-accent">
          채택된 답글
        </span>
      )}
      <div className="flex items-center justify-between">
        <span className="text-[13px] font-bold">{reply.nickname}</span>
        {currentNickname === reply.nickname && (
          <button type="button" className="text-xs text-ink-muted" onClick={() => onDeleteReply(reply.id)}>
            삭제
          </button>
        )}
      </div>
      <p className="text-sm leading-relaxed break-words whitespace-pre-wrap">{reply.content}</p>

      <button type="button" className="self-start text-xs font-medium text-ink-soft" onClick={toggleOpen}>
        댓글 {hasLoaded ? comments.length : ""} {isOpen ? "숨기기" : "보기"}
      </button>

      {isOpen && (
        <div className="flex flex-col gap-2 pt-1">
          {isLoading && <p className="py-2 text-sm text-ink-muted">불러오는 중...</p>}
          {error && <p className="text-sm text-red-500">{error}</p>}

          {!isLoading && hasLoaded && comments.length === 0 && (
            <p className="py-2 text-sm text-ink-muted">첫 댓글을 작성해주세요</p>
          )}

          <ul>
            {comments.map((c) => (
              <li key={c.id} className="flex gap-2.5 border-b border-border py-2.5 last:border-none">
                <div className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-surface-strong text-xs font-bold text-ink-soft">
                  {c.nickname.slice(0, 1)}
                </div>
                <div className="flex flex-1 flex-col gap-1">
                  <div className="flex items-center justify-between">
                    <div className="flex items-baseline gap-2">
                      <span className="text-[13px] font-bold">{c.nickname}</span>
                      <span className="text-xs text-ink-muted">{formatRelativeTime(c.createdAt)}</span>
                    </div>
                    {currentNickname === c.nickname && (
                      <button
                        type="button"
                        className="text-xs text-ink-muted"
                        onClick={() => handleDeleteComment(c.id)}
                      >
                        삭제
                      </button>
                    )}
                  </div>
                  <p className="text-sm leading-relaxed break-words whitespace-pre-wrap">{c.content}</p>
                </div>
              </li>
            ))}
          </ul>

          <form onSubmit={handleAddComment} className="flex gap-2">
            <input
              type="text"
              placeholder="댓글을 입력하세요..."
              value={draft}
              onChange={(e) => setDraft(e.target.value)}
              disabled={isSubmitting}
              className="flex-1 rounded-lg bg-surface px-3 py-2.5 text-sm outline-none"
            />
            <button
              type="submit"
              disabled={!draft.trim() || isSubmitting}
              className="rounded-lg bg-surface px-4 text-sm font-semibold disabled:opacity-40"
            >
              등록
            </button>
          </form>
        </div>
      )}
    </div>
  );
}
