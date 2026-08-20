"use client";

import { useEffect, useState } from "react";
import { cancelPayment, getPaymentRefundEligibility } from "@/lib/api/payment";
import { ApiError } from "@/lib/api/client";
import { useToastStore } from "@/store/toastStore";

// 환불 가능 여부는 항상 백엔드 응답을 그대로 표시한다.
// 현재 시각 기준 24시간 경과 여부, 수강권 사용 여부 등은 프론트에서 계산하지 않는다.
const REASON_LABEL = {
  REFUND_DEADLINE_EXCEEDED: "클래스 신청 후 24시간이 지나 환불할 수 없습니다.",
  PASS_ALREADY_USED: "이미 수강권을 사용하여 환불할 수 없습니다.",
  ALREADY_CANCELLED: "이미 취소된 결제예요.",
};

export function CancelPaymentButton({ payment, onCancelled }) {
  const showToast = useToastStore((s) => s.showToast);
  const [eligibility, setEligibility] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isCancelling, setIsCancelling] = useState(false);

  useEffect(() => {
    let cancelled = false;
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setIsLoading(true);
    getPaymentRefundEligibility(payment.id)
      .then((res) => {
        if (!cancelled) setEligibility(res);
      })
      .catch(() => {
        if (!cancelled) setEligibility(null);
      })
      .finally(() => {
        if (!cancelled) setIsLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [payment.id]);

  async function handleCancel() {
    if (!window.confirm("결제를 전체 취소할까요? 부분 취소는 지원하지 않아요.")) return;

    setIsCancelling(true);
    try {
      const result = await cancelPayment(payment.id, {});
      showToast("결제가 취소되었어요.");
      onCancelled?.(result);
    } catch (err) {
      showToast(err instanceof ApiError ? err.message : "결제 취소에 실패했어요. 다시 시도해주세요.", "error");
    } finally {
      setIsCancelling(false);
    }
  }

  if (isLoading) return null;

  if (!eligibility?.refundable) {
    const reason = eligibility?.reason ? REASON_LABEL[eligibility.reason] : null;
    return <p className="text-xs text-ink-muted">{reason ?? "이 결제는 환불할 수 없어요."}</p>;
  }

  return (
    <button
      type="button"
      onClick={handleCancel}
      disabled={isCancelling}
      className="h-[42px] w-full rounded-xl bg-surface text-[13px] font-semibold text-red-600 disabled:opacity-50"
    >
      {isCancelling ? "취소 처리 중..." : "전체 취소"}
    </button>
  );
}
