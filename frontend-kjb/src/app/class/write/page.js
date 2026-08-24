"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { AppShell, PageHeader } from "@/components/AppShell";
import { RequireAuth } from "@/components/RequireAuth";
import { BackIcon, CheckIcon } from "@/components/icons";
import { createOneDayClass } from "@/lib/api/onedayclass";
import { ApiError } from "@/lib/api/client";
import { useAuthStore } from "@/store/authStore";
import { useToastStore } from "@/store/toastStore";

function ClassWriteView() {
  const router = useRouter();
  const isHost = useAuthStore((s) => s.isHost);
  const authLoading = useAuthStore((s) => s.isLoading);
  const showToast = useToastStore((s) => s.showToast);

  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [date, setDate] = useState("");
  const [location, setLocation] = useState("");
  const [capacity, setCapacity] = useState("");
  const [price, setPrice] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (authLoading) return;
    if (!isHost) {
      router.replace("/class");
    }
  }, [authLoading, isHost, router]);

  const isValid =
    title.trim() && description.trim() && date && location.trim() && Number(capacity) > 0 && Number(price) > 0;

  async function handleSubmit(e) {
    e.preventDefault();
    if (!isValid) {
      setError("모든 항목을 입력해주세요.");
      return;
    }

    setIsSubmitting(true);
    setError(null);
    try {
      const created = await createOneDayClass({
        title: title.trim(),
        description: description.trim(),
        date,
        location: location.trim(),
        capacity: Number(capacity),
        price: Number(price),
      });
      showToast("클래스가 등록됐어요.");
      router.replace(`/class/${created.id}`);
    } catch (err) {
      showToast(err instanceof ApiError ? err.message : "클래스 등록에 실패했어요.", "error");
    } finally {
      setIsSubmitting(false);
    }
  }

  if (authLoading || !isHost) {
    return (
      <AppShell showNav={false} header={<PageHeader title="클래스 등록" />}>
        <p className="py-3 text-center text-sm text-ink-muted">불러오는 중...</p>
      </AppShell>
    );
  }

  return (
    <AppShell
      showNav={false}
      header={
        <PageHeader
          title="클래스 등록"
          left={
            <button
              type="button"
              className="flex h-8 w-8 items-center justify-center rounded-lg"
              onClick={() => router.back()}
              aria-label="닫기"
            >
              <BackIcon />
            </button>
          }
          right={
            <button
              type="button"
              className="flex h-8 w-8 items-center justify-center rounded-lg disabled:opacity-40"
              onClick={handleSubmit}
              disabled={isSubmitting || !isValid}
              aria-label="완료"
            >
              <CheckIcon />
            </button>
          }
        />
      }
    >
      <form onSubmit={handleSubmit} className="flex min-w-0 flex-1 flex-col gap-5">
        {error && <p className="rounded-lg bg-red-50 px-3.5 py-2.5 text-[13px] text-red-700">{error}</p>}

        <input
          type="text"
          placeholder="클래스 제목"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          className="w-full border-b border-border bg-transparent py-3 text-[17px] font-bold outline-none placeholder:font-medium placeholder:text-ink-muted"
        />

        <textarea
          placeholder="클래스 소개를 작성해주세요."
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          className="min-h-[160px] w-full resize-none rounded-xl border border-border bg-white p-3.5 text-[15px] leading-relaxed outline-none placeholder:text-ink-muted"
        />

        <div className="flex min-w-0 flex-col gap-2">
          <span className="text-sm font-semibold">일정</span>
          <input
            type="datetime-local"
            value={date}
            onChange={(e) => setDate(e.target.value)}
            className="w-full min-w-0 rounded-xl border border-border bg-white px-3.5 py-3 text-[15px] outline-none"
          />
        </div>

        <div className="flex min-w-0 flex-col gap-2">
          <span className="text-sm font-semibold">장소</span>
          <input
            type="text"
            placeholder="예: Seoul Farm 1"
            value={location}
            onChange={(e) => setLocation(e.target.value)}
            className="w-full rounded-xl border border-border bg-white px-3.5 py-3 text-[15px] outline-none placeholder:text-ink-muted"
          />
        </div>

        <div className="flex gap-3">
          <div className="flex min-w-0 flex-1 flex-col gap-2">
            <span className="text-sm font-semibold">정원</span>
            <input
              type="number"
              min="1"
              placeholder="명"
              value={capacity}
              onChange={(e) => setCapacity(e.target.value)}
              className="w-full min-w-0 rounded-xl border border-border bg-white px-3.5 py-3 text-[15px] outline-none placeholder:text-ink-muted"
            />
          </div>
          <div className="flex min-w-0 flex-1 flex-col gap-2">
            <span className="text-sm font-semibold">가격</span>
            <input
              type="number"
              min="1"
              placeholder="원"
              value={price}
              onChange={(e) => setPrice(e.target.value)}
              className="w-full min-w-0 rounded-xl border border-border bg-white px-3.5 py-3 text-[15px] outline-none placeholder:text-ink-muted"
            />
          </div>
        </div>
      </form>
    </AppShell>
  );
}

export default function ClassWritePage() {
  return (
    <RequireAuth>
      <ClassWriteView />
    </RequireAuth>
  );
}
