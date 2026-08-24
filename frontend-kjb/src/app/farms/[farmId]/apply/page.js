"use client";

import { useCallback, useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { AppShell, PageHeader } from "@/components/AppShell";
import { RequireAuth } from "@/components/RequireAuth";
import { BackIcon } from "@/components/icons";
import { getFarm } from "@/lib/api/farm";
import { applyRental } from "@/lib/api/rental";
import { API_BASE_URL, ApiError } from "@/lib/api/client";
import { formatCurrency } from "@/utils/format";
import { useToastStore } from "@/store/toastStore";

function FarmApplyView() {
  const { farmId } = useParams();
  const id = Number(farmId);
  const router = useRouter();
  const showToast = useToastStore((s) => s.showToast);

  const [farm, setFarm] = useState(null);
  const [message, setMessage] = useState("");
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

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

  async function handleApply() {
    setIsSubmitting(true);
    try {
      await applyRental(id, { description: message.trim() || null });
      showToast("임대 신청이 완료됐어요.");
      router.replace(`/farms/${id}`);
    } catch (err) {
      showToast(err instanceof ApiError ? err.message : "임대 신청에 실패했어요.", "error");
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <AppShell
      header={
        <PageHeader
          title="임대 신청"
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
          <div className="flex gap-3 rounded-2xl bg-surface p-3">
            {farm.thumbnailUrl ? (
              <img
                src={`${API_BASE_URL}${farm.thumbnailUrl}`}
                alt={farm.title}
                className="h-20 w-20 shrink-0 rounded-xl object-cover"
              />
            ) : (
              <div className="h-20 w-20 shrink-0 rounded-xl bg-gradient-to-br from-emerald-200 to-emerald-100" />
            )}
            <div className="flex flex-1 flex-col justify-center gap-1">
              <p className="truncate text-[15px] font-bold">{farm.title}</p>
              <p className="text-[13px] text-ink-muted">{farm.location}</p>
              <p className="text-[13px] text-ink-muted">{farm.area}㎡</p>
              <p className="text-[14px] font-bold text-ink">월 {formatCurrency(farm.monthlyRent)}</p>
            </div>
          </div>

          <div className="flex flex-col gap-2">
            <span className="text-sm font-semibold text-ink">신청 메시지</span>
            <textarea
              value={message}
              onChange={(e) => setMessage(e.target.value)}
              placeholder="집주인에게 전달할 메시지를 입력해주세요 (선택사항)"
              rows={4}
              className="w-full resize-none rounded-xl bg-surface px-4 py-3.5 text-[15px] outline-none"
            />
          </div>

          <p className="rounded-xl bg-surface px-4 py-3.5 text-[13px] text-ink-muted">
            신청하시면 별도 결제 절차 없이 바로 임대가 확정돼요.
          </p>

          <button
            type="button"
            onClick={handleApply}
            disabled={isSubmitting || farm.farmStatus !== "AVAILABLE"}
            className="mt-2 h-[50px] w-full rounded-xl bg-primary text-[15px] font-semibold text-white disabled:opacity-50 cursor-pointer disabled:cursor-not-allowed"
          >
            {isSubmitting ? "신청 중..." : "신청하기"}
          </button>
        </>
      )}
    </AppShell>
  );
}

export default function FarmApplyPage() {
  return (
    <RequireAuth>
      <FarmApplyView />
    </RequireAuth>
  );
}