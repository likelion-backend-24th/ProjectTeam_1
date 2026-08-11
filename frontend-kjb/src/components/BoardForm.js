"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { AppShell, PageHeader } from "@/components/AppShell";
import { CheckIcon, CloseIcon } from "@/components/icons";
import { createBoard, getBoard, updateBoard } from "@/lib/api/board";
import { ApiError } from "@/lib/api/client";
import { CATEGORY_LABEL } from "@/utils/format";
import { useAuthStore } from "@/store/authStore";
import { useToastStore } from "@/store/toastStore";

const BASE_CATEGORIES = ["QNA", "FREE"];

export function BoardForm({ boardId }) {
  const isEdit = boardId !== undefined;
  const router = useRouter();
  const isAdmin = useAuthStore((s) => s.isAdmin);
  const showToast = useToastStore((s) => s.showToast);
  const selectableCategories = isAdmin ? ["NOTICE", ...BASE_CATEGORIES] : BASE_CATEGORIES;

  const [category, setCategory] = useState("QNA");
  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");
  const [isLoading, setIsLoading] = useState(isEdit);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!isEdit) return;
    getBoard(boardId)
      .then((post) => {
        setCategory(post.category);
        setTitle(post.title);
        setContent(post.content);
      })
      .catch((err) => setError(err instanceof ApiError ? err.message : "게시글을 불러오지 못했어요."))
      .finally(() => setIsLoading(false));
  }, [boardId, isEdit]);

  async function handleSubmit(e) {
    e.preventDefault();
    if (!title.trim() || !content.trim()) {
      setError("제목과 내용을 모두 입력해주세요.");
      return;
    }

    setIsSubmitting(true);
    setError(null);
    try {
      if (isEdit) {
        await updateBoard(boardId, { title: title.trim(), content: content.trim(), category });
        showToast("게시글이 수정되었어요.");
        // Replace so the edit form doesn't linger in history behind the detail page.
        router.replace(`/board/${boardId}`);
      } else {
        const created = await createBoard({ title: title.trim(), content: content.trim(), category });
        showToast("게시글이 등록되었어요.");
        // Replace so pressing back from the new post goes to the board list, not this form.
        router.replace(`/board/${created.id}`);
      }
    } catch (err) {
      showToast(err instanceof ApiError ? err.message : "게시글 등록에 실패했어요.", "error");
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <AppShell
      showNav={false}
      header={
        <PageHeader
          title={isEdit ? "게시글 수정" : "게시글 등록"}
          left={
            <button className="flex h-8 w-8 items-center justify-center rounded-lg" onClick={() => router.back()} aria-label="닫기">
              <CloseIcon />
            </button>
          }
          right={
            <button
              className="flex h-8 w-8 items-center justify-center rounded-lg disabled:opacity-40"
              onClick={handleSubmit}
              disabled={isSubmitting || !title.trim() || !content.trim()}
              aria-label="완료"
            >
              <CheckIcon />
            </button>
          }
        />
      }
    >
      {isLoading ? (
        <p className="text-center text-sm text-ink-muted">불러오는 중...</p>
      ) : (
        <form onSubmit={handleSubmit} className="flex flex-1 flex-col gap-5">
          {error && <p className="rounded-lg bg-red-50 px-3.5 py-2.5 text-[13px] text-red-700">{error}</p>}

          <div className="flex flex-col gap-2">
            <span className="text-sm font-semibold">카테고리</span>
            <div className="flex gap-2">
              {selectableCategories.map((c) => (
                <button
                  key={c}
                  type="button"
                  onClick={() => setCategory(c)}
                  className={`rounded-full border px-4.5 py-2.5 text-sm font-bold ${
                    category === c ? "border-accent bg-accent text-white" : "border-border bg-white text-ink-soft"
                  }`}
                >
                  {CATEGORY_LABEL[c]}
                </button>
              ))}
            </div>
          </div>

          <input
            type="text"
            placeholder="제목"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            className="border-b border-border bg-transparent py-3 text-[17px] font-bold outline-none placeholder:font-medium placeholder:text-ink-muted"
          />

          <textarea
            placeholder={
              "글 내용을 작성해주세요!\n궁금한 내용이나, 공유하고 싶은 정보를 알려주세요.\n구체적일수록 이웃에게 도움이 돼요!"
            }
            value={content}
            onChange={(e) => setContent(e.target.value)}
            className="min-h-[320px] flex-1 resize-none bg-transparent text-[15px] leading-relaxed outline-none placeholder:text-ink-muted"
          />
        </form>
      )}
    </AppShell>
  );
}
