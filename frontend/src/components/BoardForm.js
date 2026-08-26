"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { AppShell, PageHeader } from "@/components/AppShell";
import { CheckIcon, CloseIcon, ImageIcon } from "@/components/icons";
import { createBoard, getBoard, updateBoard } from "@/lib/api/board";
import { ApiError, API_BASE_URL } from "@/lib/api/client";
import { CATEGORY_LABEL } from "@/utils/format";
import { useAuthStore } from "@/store/authStore";
import { useToastStore } from "@/store/toastStore";

const BASE_CATEGORIES = ["QNA", "FREE"];
const MAX_IMAGES = 5;

function toAbsoluteImageUrl(url) {
  return url.startsWith("http") ? url : `${API_BASE_URL}${url}`;
}

export function BoardForm({ boardId }) {
  const isEdit = boardId !== undefined;
  const router = useRouter();
  const isAdmin = useAuthStore((s) => s.isAdmin);
  const showToast = useToastStore((s) => s.showToast);
  const selectableCategories = isAdmin ? ["NOTICE", ...BASE_CATEGORIES] : BASE_CATEGORIES;

  const [category, setCategory] = useState("QNA");
  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");
  const [existingImageUrls, setExistingImageUrls] = useState([]);
  const [newImages, setNewImages] = useState([]); // { file, previewUrl }
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
        setExistingImageUrls(post.imageUrls ?? []);
      })
      .catch((err) => setError(err instanceof ApiError ? err.message : "게시글을 불러오지 못했어요."))
      .finally(() => setIsLoading(false));
  }, [boardId, isEdit]);

  // Revoke object URLs for local previews when they're replaced/unmounted.
  useEffect(() => {
    return () => newImages.forEach((img) => URL.revokeObjectURL(img.previewUrl));
  }, [newImages]);

  const imageCount = newImages.length > 0 ? newImages.length : existingImageUrls.length;

  function handleImagesSelected(e) {
    const files = Array.from(e.target.files ?? []);
    e.target.value = "";
    if (files.length === 0) return;

    setNewImages((prev) => {
      const combined = [...prev, ...files.map((file) => ({ file, previewUrl: URL.createObjectURL(file) }))];
      if (combined.length > MAX_IMAGES) {
        showToast(`이미지는 최대 ${MAX_IMAGES}장까지 첨부할 수 있어요.`, "error");
        return combined.slice(0, MAX_IMAGES);
      }
      return combined;
    });
  }

  function removeNewImage(index) {
    setNewImages((prev) => {
      const target = prev[index];
      if (target) URL.revokeObjectURL(target.previewUrl);
      return prev.filter((_, i) => i !== index);
    });
  }

  async function handleSubmit(e) {
    e.preventDefault();
    if (!title.trim() || !content.trim()) {
      setError("제목과 내용을 모두 입력해주세요.");
      return;
    }

    setIsSubmitting(true);
    setError(null);
    const files = newImages.map((img) => img.file);
    try {
      if (isEdit) {
        await updateBoard(boardId, { title: title.trim(), content: content.trim(), category }, files);
        showToast("게시글이 수정되었어요.");
        // Replace so the edit form doesn't linger in history behind the detail page.
        router.replace(`/board/${boardId}`);
      } else {
        const created = await createBoard({ title: title.trim(), content: content.trim(), category }, files);
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
            className="min-h-[200px] flex-1 resize-none bg-transparent text-[15px] leading-relaxed outline-none placeholder:text-ink-muted"
          />

          <div className="flex flex-col gap-2">
            <div className="flex items-center justify-between">
              <span className="text-sm font-semibold">사진</span>
              <span className="text-xs text-ink-muted">{imageCount}/{MAX_IMAGES}</span>
            </div>

            {isEdit && existingImageUrls.length > 0 && newImages.length === 0 && (
              <p className="text-xs text-ink-muted">새 사진을 추가하면 기존 사진이 모두 교체돼요.</p>
            )}

            <div className="flex flex-wrap gap-2.5">
              {(newImages.length > 0
                ? newImages.map((img) => img.previewUrl)
                : existingImageUrls.map(toAbsoluteImageUrl)
              ).map((src, i) => (
                <div key={src} className="relative h-20 w-20 shrink-0 overflow-hidden rounded-xl bg-surface">
                  {/* eslint-disable-next-line @next/next/no-img-element */}
                  <img src={src} alt="" className="h-full w-full object-cover" />
                  {newImages.length > 0 && (
                    <button
                      type="button"
                      onClick={() => removeNewImage(i)}
                      aria-label="사진 삭제"
                      className="absolute top-1 right-1 flex h-5 w-5 items-center justify-center rounded-full bg-black/60 text-white"
                    >
                      <CloseIcon size={12} />
                    </button>
                  )}
                </div>
              ))}

              {imageCount < MAX_IMAGES && (
                <label className="flex h-20 w-20 shrink-0 cursor-pointer flex-col items-center justify-center gap-1 rounded-xl border border-dashed border-border text-ink-muted">
                  <ImageIcon size={20} />
                  <span className="text-[11px] font-medium">추가</span>
                  <input type="file" accept="image/*" multiple className="hidden" onChange={handleImagesSelected} />
                </label>
              )}
            </div>
          </div>
        </form>
      )}
    </AppShell>
  );
}
