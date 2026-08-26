"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Script from "next/script";
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
  const [locationAddress, setLocationAddress] = useState("");
  const [area, setArea] = useState("");
  const [monthlyRent, setMonthlyRent] = useState("");
  const [rentalMonths, setRentalMonths] = useState("");
  const [description, setDescription] = useState("");
  const [images, setImages] = useState([]);
  const [previews, setPreviews] = useState([]);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isAddressSearchOpen, setIsAddressSearchOpen] = useState(false);

  useEffect(() => {
    const urls = images.map((file) => URL.createObjectURL(file));
    setPreviews(urls);
    return () => urls.forEach((url) => URL.revokeObjectURL(url));
  }, [images]);

  function handleSearchAddress() {
    if (typeof window === "undefined" || !window.daum?.Postcode) {
      showToast("주소 검색을 불러오는 중이에요. 잠시 후 다시 시도해주세요.", "error");
      return;
    }
    setIsAddressSearchOpen(true);
  }

  useEffect(() => {
    if (!isAddressSearchOpen) return;
    const container = document.getElementById("address-search-embed");
    if (!window.daum?.Postcode || !container) return;
    new window.daum.Postcode({
      oncomplete: function (data) {
        const fullAddress = data.roadAddress || data.jibunAddress || data.address;
        setLocation(fullAddress);
        setIsAddressSearchOpen(false);
      },
      width: "100%",
      height: "100%",
    }).embed(container);
  }, [isAddressSearchOpen]);

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
          locationAddress: locationAddress.trim() || null,
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
    <>
    <Script src="https://t1.daumcdn.net/mapjsapi/bundle/postcode/prod/postcode.v2.js" strategy="afterInteractive" />
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
      
      {isAddressSearchOpen && (
        <div className="absolute inset-0 z-50 flex flex-col bg-white">
          <div className="flex items-center justify-between border-b border-border px-4 py-3.5">
            <span className="text-[16px] font-bold text-ink">주소 검색</span>
            <button type="button" onClick={() => setIsAddressSearchOpen(false)} className="cursor-pointer">
              <CloseIcon size={20} />
            </button>
          </div>
          <div id="address-search-embed" className="flex-1" />
        </div>
      )}
            
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
          <div className="flex gap-2">
            <input
              type="text"
              value={location}
              readOnly
              placeholder="주소 검색 버튼을 눌러주세요"
              className="flex-1 rounded-xl bg-surface-strong px-4 py-3.5 text-[15px] text-ink-soft outline-none"
            />
            <button
              type="button"
              onClick={handleSearchAddress}
              className="shrink-0 cursor-pointer rounded-xl bg-free px-4 text-[14px] font-semibold whitespace-nowrap text-white"
            >
              주소 검색
            </button>
          </div>
        </Field>

        <Field label="상세 주소">
          <input
            type="text"
            value={locationAddress}
            onChange={(e) => setLocationAddress(e.target.value)}
            placeholder="동/호수, 건물명 등 상세 주소 (선택)"
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
    </>
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