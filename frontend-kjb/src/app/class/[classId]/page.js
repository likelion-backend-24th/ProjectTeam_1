"use client";

import { useCallback, useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { AppShell, PageHeader } from "@/components/AppShell";
import { BackIcon, LocationIcon, UsersIcon } from "@/components/icons";
import { getClassDetail, enrollClassWithPass } from "@/lib/api/onedayclass";
import { createOrder } from "@/lib/api/order";
import { ApiError } from "@/lib/api/client";
import { useAuthStore } from "@/store/authStore";
import { useToastStore } from "@/store/toastStore";
import { formatCurrency, formatDateTime } from "@/utils/format";

export default function OneDayClassDetailPage() {
  const params = useParams();
  const router = useRouter();
  const classId = params.classId;
  const showToast = useToastStore((s) => s.showToast);
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);
  const authLoading = useAuthStore((s) => s.isLoading);
  const profile = useAuthStore((s) => s.profile);

  const [cls, setCls] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);
  const [isCreatingOrder, setIsCreatingOrder] = useState(false);
  const [isEnrollingWithPass, setIsEnrollingWithPass] = useState(false);
  const [passEnrollSuccess, setPassEnrollSuccess] = useState(false);

  const load = useCallback(
    async (signal) => {
      setIsLoading(true);
      setError(null);
      try {
        const data = await getClassDetail(classId, { signal });
        if (!signal?.aborted) setCls(data);
      } catch (err) {
        if (!signal?.aborted) {
          setError(err instanceof ApiError ? err.message : "클래스 정보를 불러오지 못했어요.");
        }
      } finally {
        if (!signal?.aborted) setIsLoading(false);
      }
    },
    [classId],
  );

  useEffect(() => {
    const controller = new AbortController();
    // eslint-disable-next-line react-hooks/set-state-in-effect
    load(controller.signal);
    return () => controller.abort();
  }, [load]);

  async function handleGeneralApply() {
    setIsCreatingOrder(true);
    try {
      const order = await createOrder({ classId: Number(classId), orderType: "GENERAL" });
      router.push(`/payment/checkout?orderId=${order.id}`);
    } catch (err) {
      showToast(err instanceof ApiError ? err.message : "신청에 실패했어요. 다시 시도해주세요.", "error");
    } finally {
      setIsCreatingOrder(false);
    }
  }

  async function handlePassApply() {
    setIsEnrollingWithPass(true);
    try {
      await enrollClassWithPass(classId);
      setPassEnrollSuccess(true);
      showToast("구독 수강권으로 신청이 완료됐어요.", "success");
    } catch (err) {
      showToast(err instanceof ApiError ? err.message : "신청에 실패했어요. 다시 시도해주세요.", "error");
    } finally {
      setIsEnrollingWithPass(false);
    }
  }

  const isOwnClass = !!profile && cls?.hostId != null && profile.id === cls.hostId;

  return (
    <AppShell
      header={
        <PageHeader
          title="클래스 상세"
          left={
            <button
              type="button"
              className="flex h-8 w-8 items-center justify-center rounded-lg"
              onClick={() => router.back()}
              aria-label="뒤로가기"
            >
              <BackIcon />
            </button>
          }
        />
      }
    >
      {isLoading && <p className="py-3 text-center text-sm text-ink-muted">불러오는 중...</p>}
      {error && <p className="rounded-lg bg-red-50 px-3.5 py-2.5 text-[13px] text-red-700">{error}</p>}

      {!isLoading && !error && cls && (
        <>
          <div className="flex flex-col gap-3 rounded-[18px] border border-border bg-white p-5">
            <p className="text-[19px] font-extrabold">{cls.title}</p>
            <p className="text-[13px] text-ink-soft whitespace-pre-line">{cls.description}</p>
            <hr className="border-border" />
            <Row label="호스트" value={cls.hostNickname} />
            <Row label="일정" value={formatDateTime(cls.date)} />
            <Row
              label="장소"
              value={
                <span className="flex items-center gap-1">
                  <LocationIcon size={14} /> {cls.location}
                </span>
              }
            />
            <Row
              label="정원"
              value={
                <span className="flex items-center gap-1">
                  <UsersIcon size={14} /> {cls.enrolledCount ?? 0}/{cls.capacity}명
                </span>
              }
            />
            <Row label="가격" value={formatCurrency(cls.price)} strong />
          </div>

          {authLoading && null}

          {!authLoading && !isAuthenticated && (
            <button
              type="button"
              onClick={() => router.push(`/login?from=${encodeURIComponent(`/class/${classId}`)}`)}
              className="h-[50px] w-full rounded-xl bg-primary text-[15px] font-semibold text-white"
            >
              로그인하고 신청하기
            </button>
          )}

          {!authLoading && isAuthenticated && isOwnClass && (
            <p className="rounded-lg bg-surface px-3.5 py-3 text-center text-[13px] font-semibold text-ink-muted">
              본인이 개설한 클래스는 신청할 수 없어요.
            </p>
          )}

          {!authLoading && isAuthenticated && !isOwnClass && passEnrollSuccess && (
            <p className="rounded-lg bg-surface px-3.5 py-3 text-center text-[13px] font-semibold text-ink">
              신청이 완료됐어요. 마이페이지에서 신청 내역을 확인하세요.
            </p>
          )}

          {!authLoading && isAuthenticated && !isOwnClass && !passEnrollSuccess && (
            <div className="flex flex-col gap-2">
              <button
                type="button"
                onClick={handleGeneralApply}
                disabled={isCreatingOrder}
                className="h-[50px] w-full rounded-xl bg-primary text-[15px] font-semibold text-white disabled:opacity-50"
              >
                {isCreatingOrder ? "처리 중..." : `${formatCurrency(cls.price)} 결제하고 신청하기`}
              </button>
              <button
                type="button"
                onClick={handlePassApply}
                disabled={isEnrollingWithPass}
                className="h-[50px] w-full rounded-xl border border-primary/30 text-[15px] font-semibold text-primary disabled:opacity-50"
              >
                {isEnrollingWithPass ? "처리 중..." : "구독 수강권으로 신청하기"}
              </button>
            </div>
          )}
        </>
      )}
    </AppShell>
  );
}

function Row({ label, value, strong }) {
  return (
    <div className="flex items-center justify-between text-[13px]">
      <span className="text-ink-muted">{label}</span>
      <span className={strong ? "text-[15px] font-extrabold" : "font-semibold"}>{value}</span>
    </div>
  );
}
