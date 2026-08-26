"use client";

import { useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { AppShell, PageHeader } from "@/components/AppShell";
import { RequireAuth } from "@/components/RequireAuth";
import { BackIcon, CloseIcon } from "@/components/icons";
import { getFarm, updateFarm } from "@/lib/api/farm";
import { API_BASE_URL, ApiError } from "@/lib/api/client";
import { useToastStore } from "@/store/toastStore";

const MAX_IMAGES = 5;

function FarmEditView() {
  const { farmId } = useParams();
  const id = Number(farmId);
  const router = useRouter();
  const showToast = useToastStore((s) => s.showToast);

  const [isLoading, setIsLoading] = useState(true);
  const [title, setTitle] = useState("");
  const [area, setArea] = useState("");
  const [monthlyRent, setMonthlyRent] = useState("");
  const [rentalMonths, setRentalMonths] = useState("");
  const [description, setDescription] = useState("");
  const [existingImages, setExistingImages] = useState([]);
  const [newImages, setNewImages] = useState([]);
  const [previews, setPreviews] = useState([]);
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    if (!Number.isFinite(id)) return;
    const controller = new AbortController();
    async function load() {
      setIsLoading(true);
      try {
        const farm = await getFarm(id, controller.signal);
        if (controller.signal.aborted) return;
        setTitle(farm.title ?? "");
        setArea(String(farm.area ?? ""));
        setMonthlyRent(String(farm.monthlyRent ?? ""));
        setRentalMonths(String(farm.rentalMonths ?? ""));
        setDescription(farm.description ?? "");
        setExistingImages(farm.imageUrls ?? []);
      } catch (err) {
        if (!controller.signal.aborted) {
          showToast(err instanceof ApiError ? err.message : "밭 정보를 불러오지 못했어요.", "error");
        }
      } finally {
        if (!controller.signal.aborted) setIsLoading(false);
      }
    }
    load();
    return () => controller.abort();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  useEffect(() => {
    const urls = newImages.map((file) => URL.createObjectURL(file));
    setPreviews(urls);
    return () => urls.forEach((url) => URL.revokeObjectURL(url));
  }, [newImages]);

  function handleImageSelect(e) {
    const files = Array.from(e.target.files ?? []);
    if (newImages.length + files.length > MAX_IMAGES) {
      showToast(`사진은 최대 ${MAX_IMAGES}장까지 등록할 수 있어요.`, "error");
      e.target.value = "";
      return;
    }
    setNewImages((prev) => [...prev, ...files]);
    e.target.value = "";
  }

  function handleRemoveNewImage(index) {
    setNewImages((prev) => prev.filter((_, i) => i !== index));
  }

  async function handleSubmit(e) {
    e.preventDefault();
    if (!title.trim() || !area || !monthlyRent || !rentalMonths) {
      showToast("필수 항목을 모두 입력해주세요.", "error");
      return;
    }

    setIsSubmitting(true);
    try {
      await updateFarm(
        id,
        {
          title: title.trim(),
          area: Number(area),
          monthlyRent: Number(monthlyRent),
          rentalMonths: Number(rentalMonths),
          description: description.trim() || null,
        },
        newImages,
      );
      showToast("밭 정보가 수정되었어요.");
      router.replace(`/farms/${id}`);
    } catch (err) {
      showToast(err instanceof ApiError ? err.message : "밭 수정에 실패했어요.", "error");
    } finally {
      setIsSubmitting(false);
    }
  }

  if (isLoading) {
    return (
      <AppShell
        header={
          <PageHeader
            title="밭 정보 수정"
            left={
              <button type="button" onClick={() => router.back()} className="cursor-pointer">
                <BackIcon size={22} />
              </button>
            }
          />
        }
      >
        <p className="text-center text-sm text-ink-muted">불러오는 중...</p>
      </AppShell>
    );
  }

  return (
    <AppShell
      header={
        <PageHeader
          title="밭 정보 수정"
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

        <Field label="면적 (㎡)" required>
          <input
            type="number"
            min="1"
            value={area}
            onChange={(e) => setArea(e.target.value)}
            className="w-full rounded-xl bg-surface px-4 py-3.5 text-[15px] outline-none"
          />
        </Field>

        <Field label="월 임대료 (원)" required>
          <input
            type="number"
            min="1"
            value={monthlyRent}
            onChange={(e) => setMonthlyRent(e.target.value)}
            className="w-full rounded-xl bg-surface px-4 py-3.5 text-[15px] outline-none"
          />
        </Field>

        <Field label="임대 기간 (개월)" required>
          <input
            type="number"
            min="1"
            value={rentalMonths}
            onChange={(e) => setRentalMonths(e.target.value)}
            className="w-full rounded-xl bg-surface px-4 py-3.5 text-[15px] outline-none"
          />
        </Field>

        <Field label="밭 설명">
          <textarea
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            rows={4}
            className="w-full resize-none rounded-xl bg-surface px-4 py-3.5 text-[15px] outline-none"
          />
        </Field>

        <Field label="사진">
          {existingImages.length > 0 && newImages.length === 0 && (
            <>
              <div className="flex flex-wrap gap-2">
                {existingImages.map((url) => (
                  <img key={url} src={`${API_BASE_URL}${url}`} alt="" className="h-20 w-20 rounded-xl object-cover" />
                ))}
              </div>
              <p className="mt-1.5 text-[12px] text-ink-muted">
                현재 등록된 사진이에요. 새 사진을 추가하면 기존 사진은 전부 교체돼요.
              </p>
            </>
          )}

          <div className="mt-2 flex flex-wrap gap-2">
            {previews.map((url, i) => (
              <div key={url} className="relative h-20 w-20 shrink-0">
                <img src={url} alt="" className="h-20 w-20 rounded-xl object-cover" />
                <button
                  type="button"
                  onClick={() => handleRemoveNewImage(i)}
                  className="absolute -top-1.5 -right-1.5 flex h-5 w-5 items-center justify-center rounded-full bg-ink text-white"
                >
                  <CloseIcon size={12} />
                </button>
              </div>
            ))}
            {newImages.length < MAX_IMAGES && (
              <label className="flex h-20 w-20 shrink-0 cursor-pointer flex-col items-center justify-center gap-1 rounded-xl border border-dashed border-border text-ink-muted">
                <span className="text-xl">+</span>
                <span className="text-[11px]">사진 추가</span>
                <input type="file" accept="image/*" multiple onChange={handleImageSelect} className="hidden" />
              </label>
            )}
          </div>
        </Field>

        <button
          type="submit"
          disabled={isSubmitting}
          className="mt-2 h-[50px] w-full rounded-xl bg-primary text-[15px] font-semibold text-white disabled:opacity-50"
        >
          {isSubmitting ? "수정 중..." : "수정 완료"}
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

export default function FarmEditPage() {
  return (
    <RequireAuth>
      <FarmEditView />
    </RequireAuth>
  );
}