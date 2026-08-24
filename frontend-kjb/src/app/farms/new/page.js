"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { AppShell, PageHeader } from "@/components/AppShell";
import { RequireAuth } from "@/components/RequireAuth";
import { BackIcon, CloseIcon } from "@/components/icons";
import { createFarm } from "@/lib/api/farm";
import { ApiError } from "@/lib/api/client";
import { useToastStore } from "@/store/toastStore";

const MAX_IMAGES = 5;

function FarmNewView() {
  const router = useRouter();
  const showToast = useToastStore((s) => s.showToast);

  const [title, setTitle] = useState("");
  const [location, setLocation] = useState("");
  const [area, setArea] = useState("");
  const [monthlyRent, setMonthlyRent] = useState("");
  const [rentalMonths, setRentalMonths] = useState("");
  const [description, setDescription] = useState("");
  const [images, setImages] = useState([]);
  const [previews, setPreviews] = useState([]);
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    const urls = images.map((file) => URL.createObjectURL(file));
    setPreviews(urls);
    return () => urls.forEach((url) => URL.revokeObjectURL(url));
  }, [images]);

  function handleImageSelect(e) {
    const files = Array.from(e.target.files ?? []);
    if (images.length + files.length > MAX_IMAGES) {
      showToast(`사진은 최대 ${MAX_IMAGES}장까지 등록할 수 있어요.`, "error");
      e.target.value = "";
      return;
    }
    setImages((prev) => [...prev, ...files]);
    e.target.value = "";
  }

  function handleRemoveImage(index) {
    setImages((prev) => prev.filter((_, i) => i !== index));
  }

  async function handleSubmit(e) {
    e.preventDefault();
    if (!title.trim() || !location.trim() || !area || !monthlyRent || !rentalMonths) {
      showToast("필수 항목을 모두 입력해주세요.", "error");
      return;
    }

    setIsSubmitting(true);
    try {
      await createFarm(
        {
          title: title.trim(),
          location: location.trim(),
          locationAddress: null,
          area: Number(area),
          monthlyRent: Number(monthlyRent),
          rentalMonths: Number(rentalMonths),
          description: description.trim() || null,
        },
        images,
      );
      showToast("밭이 등록되었어요.");
      router.replace("/farms");
    } catch (err) {
      showToast(err instanceof ApiError ? err.message : "밭 등록에 실패했어요.", "error");
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <AppShell
      header={
        <PageHeader
          title="밭 등록"
          left={
            <button type="button" onClick={() => router.back()} className="cursor-pointer">
              <BackIcon size={22} />
            </button>
          }
        />
      }
    >
      <form onSubmit={handleSubmit} className="flex flex-col gap-5">
        <Field label="밭 이름" required>
          <input
            type="text"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            placeholder="밭 이름을 입력해주세요"
            className="w-full rounded-xl bg-surface px-4 py-3.5 text-[15px] outline-none"
          />
        </Field>

        <Field label="위치" required>
          <input
            type="text"
            value={location}
            onChange={(e) => setLocation(e.target.value)}
            placeholder="주소 또는 위치를 입력해주세요"
            className="w-full rounded-xl bg-surface px-4 py-3.5 text-[15px] outline-none"
          />
        </Field>

        <Field label="면적 (㎡)" required>
          <input
            type="number"
            min="1"
            value={area}
            onChange={(e) => setArea(e.target.value)}
            placeholder="면적을 입력해주세요"
            className="w-full rounded-xl bg-surface px-4 py-3.5 text-[15px] outline-none"
          />
        </Field>

        <Field label="월 임대료 (원)" required>
          <input
            type="number"
            min="1"
            value={monthlyRent}
            onChange={(e) => setMonthlyRent(e.target.value)}
            placeholder="월 임대료를 입력해주세요"
            className="w-full rounded-xl bg-surface px-4 py-3.5 text-[15px] outline-none"
          />
        </Field>

        <Field label="임대 기간 (개월)" required>
          <input
            type="number"
            min="1"
            value={rentalMonths}
            onChange={(e) => setRentalMonths(e.target.value)}
            placeholder="예: 6"
            className="w-full rounded-xl bg-surface px-4 py-3.5 text-[15px] outline-none"
          />
        </Field>

        <Field label="밭 설명">
          <textarea
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder="밭에 대한 설명을 입력해주세요 (선택사항)"
            rows={4}
            className="w-full resize-none rounded-xl bg-surface px-4 py-3.5 text-[15px] outline-none"
          />
        </Field>

        <Field label="사진 등록">
          <div className="flex flex-wrap gap-2">
            {previews.map((url, i) => (
              <div key={url} className="relative h-20 w-20 shrink-0">
                <img src={url} alt="" className="h-20 w-20 rounded-xl object-cover" />
                <button
                  type="button"
                  onClick={() => handleRemoveImage(i)}
                  className="absolute -top-1.5 -right-1.5 flex h-5 w-5 items-center justify-center rounded-full bg-ink text-white"
                >
                  <CloseIcon size={12} />
                </button>
              </div>
            ))}
            {images.length < MAX_IMAGES && (
              <label className="flex h-20 w-20 shrink-0 cursor-pointer flex-col items-center justify-center gap-1 rounded-xl border border-dashed border-border text-ink-muted">
                <span className="text-xl">+</span>
                <span className="text-[11px]">사진 추가</span>
                <input type="file" accept="image/*" multiple onChange={handleImageSelect} className="hidden" />
              </label>
            )}
          </div>
          <p className="mt-1.5 text-[12px] text-ink-muted">사진은 최대 {MAX_IMAGES}장까지 등록할 수 있어요.</p>
        </Field>

        <button
          type="submit"
          disabled={isSubmitting}
          className="mt-2 h-[50px] w-full rounded-xl bg-primary text-[15px] font-semibold text-white disabled:opacity-50"
        >
          {isSubmitting ? "등록 중..." : "등록하기"}
        </button>
      </form>
    </AppShell>
  );
}

function Field({ label, required, children }) {
  return (
    <div className="flex flex-col gap-2">
      <span className="text-sm font-semibold text-ink">
        {label}
        {required && <span className="ml-0.5 text-primary">*</span>}
      </span>
      {children}
    </div>
  );
}

export default function FarmNewPage() {
  return (
    <RequireAuth>
      <FarmNewView />
    </RequireAuth>
  );
}