"use client";

import { useCallback, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { AppShell, PageHeader } from "@/components/AppShell";
import { RequireAuth } from "@/components/RequireAuth";
import { BackIcon } from "@/components/icons";
import { BillingKeyCard } from "@/components/billing/BillingKeyCard";
import { PlanCard } from "@/components/subscription/PlanCard";
import { getMyBillingKey } from "@/lib/api/billingKey";
import { createSubscription } from "@/lib/api/subscription";
import { ApiError } from "@/lib/api/client";
import { useToastStore } from "@/store/toastStore";

const STEPS = ["플랜 확인", "카드 등록", "구독 결제", "구독 활성화"];

function StepIndicator({ current }) {
  return (
    <ol className="flex items-center gap-1.5 text-[11px] font-semibold text-ink-muted">
      {STEPS.map((step, i) => (
        <li key={step} className={`flex items-center gap-1.5 ${i <= current ? "text-ink" : ""}`}>
          <span
            className={`flex h-5 w-5 items-center justify-center rounded-full ${
              i <= current ? "bg-primary text-white" : "bg-surface"
            }`}
          >
            {i + 1}
          </span>
          {step}
          {i < STEPS.length - 1 && <span className="mx-0.5 text-ink-muted">›</span>}
        </li>
      ))}
    </ol>
  );
}

function BillingKeyRegistrationView() {
  const router = useRouter();
  const showToast = useToastStore((s) => s.showToast);

  const [billingKey, setBillingKey] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isSubscribing, setIsSubscribing] = useState(false);

  const load = useCallback(async () => {
    setIsLoading(true);
    try {
      const key = await getMyBillingKey();
      setBillingKey(key ?? null);
    } catch {
      setBillingKey(null);
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    load();
  }, [load]);

  const hasActiveCard = billingKey?.status === "ACTIVE";

  async function handleSubscribe() {
    setIsSubscribing(true);
    try {
      await createSubscription({ planType: "BASIC", billingKeyId: billingKey.id });
      showToast("구독이 시작되었어요.");
      router.replace("/subscription");
    } catch (err) {
      showToast(err instanceof ApiError ? err.message : "구독 결제에 실패했어요. 다시 시도해주세요.", "error");
    } finally {
      setIsSubscribing(false);
    }
  }

  return (
    <>
      <StepIndicator current={hasActiveCard ? 2 : 1} />

      {isLoading && <p className="py-3 text-center text-sm text-ink-muted">불러오는 중...</p>}

      {!isLoading && (
        <>
          <PlanCard />
          <BillingKeyCard billingKey={billingKey} onRegistered={load} />

          {hasActiveCard && (
            <button
              type="button"
              onClick={handleSubscribe}
              disabled={isSubscribing}
              className="h-[50px] w-full rounded-xl bg-primary text-[15px] font-semibold text-white disabled:opacity-50"
            >
              {isSubscribing ? "구독 처리 중..." : "구독 시작"}
            </button>
          )}
        </>
      )}
    </>
  );
}

export default function SubscriptionBillingPage() {
  const router = useRouter();

  return (
    <RequireAuth>
      <AppShell
        header={
          <PageHeader
            title="카드 등록"
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
        <BillingKeyRegistrationView />
      </AppShell>
    </RequireAuth>
  );
}
