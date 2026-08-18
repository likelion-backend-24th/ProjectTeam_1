"use client";

import { useState } from "react";
import { cancelSubscription } from "@/lib/api/subscription";
import { ApiError } from "@/lib/api/client";
import { useToastStore } from "@/store/toastStore";

export function CancelSubscriptionButton({ subscriptionId, onCancelled }) {
  const showToast = useToastStore((s) => s.showToast);
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleClick() {
    if (
      !window.confirm(
        "구독을 해지할까요?\n현재 구독 기간이 종료될 때 해지되며, 그 전까지는 남은 수강권을 사용할 수 있어요.",
      )
    ) {
      return;
    }

    setIsSubmitting(true);
    try {
      const updated = await cancelSubscription(subscriptionId);
      showToast("구독 해지가 예약되었어요.");
      onCancelled?.(updated);
    } catch (err) {
      showToast(err instanceof ApiError ? err.message : "구독 해지에 실패했어요. 다시 시도해주세요.", "error");
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <button
      type="button"
      onClick={handleClick}
      disabled={isSubmitting}
      className="h-[46px] w-full rounded-xl bg-surface text-[14px] font-semibold text-ink-soft disabled:opacity-50"
    >
      {isSubmitting ? "처리 중..." : "구독 해지"}
    </button>
  );
}
