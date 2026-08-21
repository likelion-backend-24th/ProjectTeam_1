"use client";

import { useState } from "react";
import { CardIcon } from "@/components/icons";
import { StatusPill } from "@/components/payment/StatusPill";
import { BILLING_KEY_STATUS_LABEL } from "@/lib/constants/status";
import { createBillingKeyIssuanceIntent, confirmBillingKeyIssuance } from "@/lib/api/billingKey";
import { requestIssueBillingKey } from "@/lib/portone/client";
import { ApiError } from "@/lib/api/client";
import { useToastStore } from "@/store/toastStore";

// 카드 등록/변경 흐름을 담당한다. 실제 카드번호는 이 컴포넌트도, 우리 서버도 다루지 않는다 —
// PortOne SDK가 반환하는 issueId/billingKey 식별자만 오간다.
export function BillingKeyCard({ billingKey, onRegistered }) {
  const showToast = useToastStore((s) => s.showToast);
  const [isRegistering, setIsRegistering] = useState(false);

  async function handleRegister() {
    setIsRegistering(true);
    try {
      const intent = await createBillingKeyIssuanceIntent();
      const result = await requestIssueBillingKey({ intent });
      const confirmed = await confirmBillingKeyIssuance({ issueId: intent.issueId, billingKey: result.billingKey });
      showToast("결제 수단이 등록되었어요.");
      onRegistered?.(confirmed, result);
    } catch (err) {
      showToast(
        err instanceof ApiError
          ? err.message
          : err?.message?.startsWith("NOT_IMPLEMENTED")
            ? "카드 등록 기능은 PortOne 연동 확정 후 사용할 수 있어요."
            : "카드 등록에 실패했어요. 다시 시도해주세요.",
        "error",
      );
    } finally {
      setIsRegistering(false);
    }
  }

  return (
    <div className="flex flex-col gap-3.5 rounded-[18px] border border-border bg-white p-5">
      <div className="flex items-center gap-2">
        <CardIcon size={20} className="text-ink-soft" />
        <p className="text-[15px] font-extrabold">자동결제 카드</p>
      </div>

      {billingKey ? (
        <div className="flex items-center justify-between rounded-xl bg-surface px-3.5 py-3.5">
          <div>
            <p className="text-[14px] font-bold">등록된 결제수단</p>
            <p className="text-xs text-ink-muted">구독 자동결제에 사용돼요</p>
          </div>
          <StatusPill status={billingKey.status} label={BILLING_KEY_STATUS_LABEL[billingKey.status]} />
        </div>
      ) : (
        <p className="rounded-xl bg-surface px-3.5 py-3.5 text-center text-[13px] text-ink-muted">
          등록된 결제수단이 없어요.
        </p>
      )}

      <button
        type="button"
        onClick={handleRegister}
        disabled={isRegistering}
        className="h-[50px] w-full rounded-xl bg-primary text-[15px] font-semibold text-white disabled:opacity-50"
      >
        {isRegistering ? "등록 중..." : billingKey ? "카드 변경" : "카드 등록"}
      </button>
    </div>
  );
}
