"use client";

import { useCallback, useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { AppShell, PageHeader } from "@/components/AppShell";
import { BackIcon, ChevronRightIcon } from "@/components/icons";
import { getFarm } from "@/lib/api/farm";
import { API_BASE_URL, ApiError } from "@/lib/api/client";
import { formatCurrency, formatDate } from "@/utils/format";
import { useAuthStore } from "@/store/authStore";

const STATUS_LABEL = {
  AVAILABLE: "임대 가능",
  RENTED: "임대중",
};

export default function FarmDetailPage() {
  const { farmId } = useParams();
  const id = Number(farmId);
  const router = useRouter();
  const profile = useAuthStore((s) => s.profile);

  const [farm, setFarm] = useState(null);
  const [imgIndex, setImgIndex] = useState(0);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);

  const load = useCallback(
    async (signal) => {
      setIsLoading(true);
      setError(null);
      try {
        const data = await getFarm(id, signal);
        if (signal?.aborted) return;
        setFarm(data);
      } catch (err) {
        if (signal?.aborted) return;
        setError(err instanceof ApiError ? err.message : "밭 정보를 불러오지 못했어요.");
      } finally {
        if (!signal?.aborted) setIsLoading(false);
      }
    },
    [id],
  );

  useEffect(() => {
    if (!Number.isFinite(id)) return;
    const controller = new AbortController();
    // eslint-disable-next-line react-hooks/set-state-in-effect
    load(controller.signal);
    return () => controller.abort();
  }, [id, load]);

  const images = farm?.imageUrls?.length ? farm.imageUrls : [];
  const isOwner = Boolean(profile?.nickname && farm?.ownerNickname && profile.nickname === farm.ownerNickname);

  function nextImage() {
    setImgIndex((i) => (i + 1) % images.length);
  }
  function prevImage() {
    setImgIndex((i) => (i - 1 + images.length) % images.length);
  }

  function handleApplyClick() {
    router.push(`/farms/${id}/apply`);
  }

  function handleEditClick(){
    router.push(`/farms/${id}/edit`);
  }

  return (
    <AppShell
      header={
        <PageHeader
          title="밭 상세 정보"
          left={
            <button type="button" onClick={() => router.back()} className="cursor-pointer">
              <BackIcon size={22} />
            </button>
          }
        />
      }
    >
      {isLoading && <p className="text-center text-sm text-ink-muted">불러오는 중...</p>}
      {error && <p className="rounded-lg bg-red-50 px-3.5 py-2.5 text-[13px] text-red-700">{error}</p>}

      {farm && (
        <>
          <div className="relative aspect-[4/3] w-full overflow-hidden rounded-2xl bg-gradient-to-br from-emerald-200 to-emerald-100">
            {images.length > 0 && (
              <img
                src={`${API_BASE_URL}${images[imgIndex]}`}
                alt={farm.title}
                className="h-full w-full object-cover"
              />
            )}
            {images.length > 1 && (
              <>
                <button
                  type="button"
                  onClick={prevImage}
                  className="absolute top-1/2 left-2 flex h-8 w-8 -translate-y-1/2 items-center justify-center rounded-full bg-black/40 text-white cursor-pointer"
                >
                  <BackIcon size={16} />
                </button>
                <button
                  type="button"
                  onClick={nextImage}
                  className="absolute top-1/2 right-2 flex h-8 w-8 -translate-y-1/2 items-center justify-center rounded-full bg-black/40 text-white cursor-pointer"
                >
                  <ChevronRightIcon size={16} />
                </button>
                <span className="absolute right-2 bottom-2 rounded-full bg-black/50 px-2 py-0.5 text-[11px] text-white">
                  {imgIndex + 1} / {images.length}
                </span>
              </>
            )}
          </div>

          <div className="flex flex-col gap-1">
            <div className="flex items-center justify-between">
              <h2 className="text-[19px] font-extrabold">{farm.title}</h2>
              <span
                className={`shrink-0 rounded-full px-2.5 py-1 text-[11px] font-semibold ${
                  farm.farmStatus === "AVAILABLE" ? "bg-primary/10 text-primary" : "bg-surface-strong text-ink-muted"
                }`}
              >
                {STATUS_LABEL[farm.farmStatus] ?? farm.farmStatus}
              </span>
            </div>
            <p className="text-[13px] text-ink-muted">{farm.location}</p>
            <p className="text-[13px] text-ink-muted">
              등록자 {farm.ownerNickname} · {formatDate(farm.createdAt)}
            </p>
          </div>

          <p className="text-[20px] font-extrabold text-ink">월 {formatCurrency(farm.monthlyRent)}</p>

          <div className="flex flex-col gap-2">
            <span className="text-[15px] font-bold text-ink">밭 설명</span>
            <p className="rounded-xl bg-surface px-4 py-3.5 text-[14px] leading-relaxed break-words whitespace-pre-wrap text-ink-soft">
              {farm.description || "등록된 설명이 없어요."}
            </p>
          </div>

          <div className="flex flex-col gap-2">
            <span className="text-[15px] font-bold text-ink">상세 정보</span>
            <div className="flex flex-col gap-2.5 rounded-xl bg-surface px-4 py-3.5 text-[14px]">
              <InfoRow label="면적" value={`${farm.area}㎡`} />
              <InfoRow label="월 임대료" value={formatCurrency(farm.monthlyRent)} />
              <InfoRow label="계약 기간" value={`${farm.rentalMonths}개월`} />
            </div>
          </div>

         {isOwner ? (
            <button
              type="button"
              onClick={handleEditClick}
              className="mt-2 h-[50px] w-full rounded-xl bg-primary text-[15px] font-semibold text-white cursor-pointer"
            >
              수정하기
            </button>
          ) : (
            <button
              type="button"
              onClick={handleApplyClick}
              disabled={farm.farmStatus !== "AVAILABLE"}
              className="mt-2 h-[50px] w-full rounded-xl bg-primary text-[15px] font-semibold text-white disabled:opacity-50 cursor-pointer disabled:cursor-not-allowed"
            >
              {farm.farmStatus === "AVAILABLE" ? "임대 신청하기" : "이미 임대중인 밭이에요"}
            </button>
          )}
        </>
      )}
    </AppShell>
  );
}

function InfoRow({ label, value }) {
  return (
    <div className="flex items-center justify-between">
      <span className="text-ink-muted">{label}</span>
      <span className="font-semibold text-ink">{value}</span>
    </div>
  );
}