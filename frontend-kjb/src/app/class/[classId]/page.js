"use client";

import { useCallback, useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { AppShell, PageHeader } from "@/components/AppShell";
import { BackIcon, LocationIcon, UsersIcon } from "@/components/icons";
import {
  getClassDetail,
  enrollClassWithPass,
  updateClassDescription,
  cancelOneDayClass,
  getClassApplicants,
} from "@/lib/api/onedayclass";
import { createOrder, getOrder } from "@/lib/api/order";
import { getMyEnrollments, cancelEnrollmentByPass } from "@/lib/api/enrollment";
import { CancelPaymentButton } from "@/components/payment/CancelPaymentButton";
import { ApiError } from "@/lib/api/client";
import { useAuthStore } from "@/store/authStore";
import { useToastStore } from "@/store/toastStore";
import { formatCurrency, formatDateTime } from "@/utils/format";

const APPLICANT_STATUS_LABEL = { PENDING: "결제대기", CONFIRMED: "확정", CANCELLED: "취소" };
const APPLICANT_PAYMENT_TYPE_LABEL = { GENERAL: "일반결제", SUBSCRIPTION: "구독수강권" };

export default function OneDayClassDetailPage() {
  const params = useParams();
  const router = useRouter();
  const classId = params.classId;
  const showToast = useToastStore((s) => s.showToast);
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);
  const authLoading = useAuthStore((s) => s.isLoading);
  const profile = useAuthStore((s) => s.profile);
  const isAdmin = useAuthStore((s) => s.isAdmin);

  const [cls, setCls] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);
  const [isCreatingOrder, setIsCreatingOrder] = useState(false);
  const [isEnrollingWithPass, setIsEnrollingWithPass] = useState(false);

  const [myEnrollment, setMyEnrollment] = useState(null);
  const [myPayment, setMyPayment] = useState(null);
  const [isEnrollmentLoading, setIsEnrollmentLoading] = useState(true);
  const [isCancellingPass, setIsCancellingPass] = useState(false);

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

  const isOwnClass = !!profile && cls?.hostId != null && profile.id === cls.hostId;
  const canManageClass = isOwnClass || isAdmin;

  const loadMyEnrollment = useCallback(async () => {
    setIsEnrollmentLoading(true);
    try {
      const list = await getMyEnrollments();
      const found = (list ?? []).find((e) => e.classId === Number(classId));
      setMyEnrollment(found ?? null);
      if (found?.orderId) {
        try {
          const order = await getOrder(found.orderId);
          setMyPayment(order?.payment?.id ? { id: order.payment.id } : null);
        } catch {
          setMyPayment(null);
        }
      } else {
        setMyPayment(null);
      }
    } catch {
      setMyEnrollment(null);
      setMyPayment(null);
    } finally {
      setIsEnrollmentLoading(false);
    }
  }, [classId]);

  useEffect(() => {
    if (authLoading || !isAuthenticated || isOwnClass) {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setIsEnrollmentLoading(false);
      return;
    }
    loadMyEnrollment();
  }, [authLoading, isAuthenticated, isOwnClass, loadMyEnrollment]);

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
      showToast("구독 수강권으로 신청이 완료됐어요.", "success");
      load();
      loadMyEnrollment();
    } catch (err) {
      showToast(err instanceof ApiError ? err.message : "신청에 실패했어요. 다시 시도해주세요.", "error");
    } finally {
      setIsEnrollingWithPass(false);
    }
  }

  async function handleCancelPassEnrollment() {
    if (!myEnrollment) return;
    if (!window.confirm("신청을 취소할까요? 사용한 수강권이 복구돼요.")) return;
    setIsCancellingPass(true);
    try {
      await cancelEnrollmentByPass(myEnrollment.enrollmentId);
      showToast("신청이 취소됐어요.");
      setMyEnrollment(null);
      setMyPayment(null);
      load();
    } catch (err) {
      showToast(err instanceof ApiError ? err.message : "취소에 실패했어요. 다시 시도해주세요.", "error");
    } finally {
      setIsCancellingPass(false);
    }
  }

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

          {!authLoading && isAuthenticated && canManageClass && (
            <HostClassActions
              classId={classId}
              description={cls.description}
              onDescriptionUpdated={(description) => setCls((prev) => ({ ...prev, description }))}
              onCancelled={() => {
                showToast("클래스가 취소됐어요.");
                router.replace("/class");
              }}
            />
          )}

          {!authLoading && isAuthenticated && !isOwnClass && !isEnrollmentLoading && myEnrollment && (
            <div className="flex flex-col gap-2">
              <p className="rounded-lg bg-surface px-3.5 py-3 text-center text-[13px] font-semibold text-ink">
                이미 신청한 클래스예요.
              </p>
              {myEnrollment.orderId ? (
                myPayment ? (
                  <CancelPaymentButton
                    payment={myPayment}
                    onCancelled={() => {
                      setMyEnrollment(null);
                      setMyPayment(null);
                      load();
                    }}
                  />
                ) : null
              ) : (
                <button
                  type="button"
                  onClick={handleCancelPassEnrollment}
                  disabled={isCancellingPass}
                  className="h-[42px] w-full rounded-xl bg-surface text-[13px] font-semibold text-red-600 disabled:opacity-50"
                >
                  {isCancellingPass ? "취소 처리 중..." : "신청 취소"}
                </button>
              )}
            </div>
          )}

          {!authLoading && isAuthenticated && !isOwnClass && !isEnrollmentLoading && !myEnrollment && (
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

function HostClassActions({ classId, description, onDescriptionUpdated, onCancelled }) {
  const showToast = useToastStore((s) => s.showToast);
  const [isEditing, setIsEditing] = useState(false);
  const [draft, setDraft] = useState(description);
  const [isSaving, setIsSaving] = useState(false);
  const [isCancelling, setIsCancelling] = useState(false);

  const [showApplicants, setShowApplicants] = useState(false);
  const [applicants, setApplicants] = useState(null);
  const [isLoadingApplicants, setIsLoadingApplicants] = useState(false);

  async function handleToggleApplicants() {
    if (showApplicants) {
      setShowApplicants(false);
      return;
    }
    setShowApplicants(true);
    if (applicants !== null) return;
    setIsLoadingApplicants(true);
    try {
      const list = await getClassApplicants(classId);
      setApplicants(list ?? []);
    } catch (err) {
      showToast(err instanceof ApiError ? err.message : "신청자 목록을 불러오지 못했어요.", "error");
      setApplicants([]);
    } finally {
      setIsLoadingApplicants(false);
    }
  }

  async function handleSaveDescription() {
    if (!draft.trim()) return;
    setIsSaving(true);
    try {
      const updated = await updateClassDescription(classId, draft.trim());
      onDescriptionUpdated(updated.description);
      showToast("설명이 수정됐어요.");
      setIsEditing(false);
    } catch (err) {
      showToast(err instanceof ApiError ? err.message : "설명 수정에 실패했어요.", "error");
    } finally {
      setIsSaving(false);
    }
  }

  async function handleCancelClass() {
    if (!window.confirm("클래스를 취소할까요? 신청자 전체가 환불/수강권 복구 처리돼요.")) return;
    setIsCancelling(true);
    try {
      await cancelOneDayClass(classId);
      onCancelled();
    } catch (err) {
      showToast(err instanceof ApiError ? err.message : "클래스 취소에 실패했어요.", "error");
    } finally {
      setIsCancelling(false);
    }
  }

  if (isEditing) {
    return (
      <div className="flex flex-col gap-2">
        <textarea
          value={draft}
          onChange={(e) => setDraft(e.target.value)}
          className="min-h-[120px] resize-none rounded-xl border border-border bg-white p-3.5 text-[14px] leading-relaxed outline-none"
        />
        <div className="flex gap-2">
          <button
            type="button"
            onClick={() => {
              setDraft(description);
              setIsEditing(false);
            }}
            className="h-[42px] flex-1 rounded-xl bg-surface text-[13px] font-semibold text-ink-soft"
          >
            취소
          </button>
          <button
            type="button"
            onClick={handleSaveDescription}
            disabled={isSaving || !draft.trim()}
            className="h-[42px] flex-1 rounded-xl bg-class text-[13px] font-semibold text-white disabled:opacity-50"
          >
            {isSaving ? "저장 중..." : "저장"}
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-2">
      <button
        type="button"
        onClick={() => {
          setDraft(description);
          setIsEditing(true);
        }}
        className="h-[42px] w-full rounded-xl border border-class/30 text-[13px] font-semibold text-class"
      >
        설명 수정
      </button>
      <button
        type="button"
        onClick={handleCancelClass}
        disabled={isCancelling}
        className="h-[42px] w-full rounded-xl bg-surface text-[13px] font-semibold text-red-600 disabled:opacity-50"
      >
        {isCancelling ? "취소 처리 중..." : "클래스 취소"}
      </button>

      <button
        type="button"
        onClick={handleToggleApplicants}
        className="h-[42px] w-full rounded-xl bg-surface text-[13px] font-semibold text-ink-soft"
      >
        {showApplicants ? "신청자 목록 숨기기" : "신청자 목록 보기"}
      </button>

      {showApplicants && (
        <div className="flex flex-col gap-2 rounded-xl border border-border bg-white p-3.5">
          {isLoadingApplicants && <p className="py-2 text-center text-[13px] text-ink-muted">불러오는 중...</p>}
          {!isLoadingApplicants && applicants?.length === 0 && (
            <p className="py-2 text-center text-[13px] text-ink-muted">아직 신청자가 없어요.</p>
          )}
          {!isLoadingApplicants &&
            applicants?.map((a) => (
              <div key={a.enrollmentId} className="flex items-center justify-between gap-2 text-[13px]">
                <div className="flex flex-col">
                  <span className="font-semibold text-ink">{a.userNickname}</span>
                  <span className="text-[11px] text-ink-muted">
                    {APPLICANT_PAYMENT_TYPE_LABEL[a.paymentType] ?? a.paymentType} · {formatDateTime(a.createdAt)}
                  </span>
                </div>
                <span
                  className={`shrink-0 rounded-full px-2.5 py-1 text-[11px] font-semibold ${
                    a.status === "CONFIRMED"
                      ? "bg-class-soft text-orange-700"
                      : a.status === "CANCELLED"
                        ? "bg-surface text-ink-muted"
                        : "bg-surface-strong text-ink-soft"
                  }`}
                >
                  {APPLICANT_STATUS_LABEL[a.status] ?? a.status}
                </span>
              </div>
            ))}
        </div>
      )}
    </div>
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
