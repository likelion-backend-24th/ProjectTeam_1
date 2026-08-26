"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { AppShell, PageHeader } from "@/components/AppShell";
import { PlanCard } from "@/components/subscription/PlanCard";
import { CurrentSubscriptionCard } from "@/components/subscription/CurrentSubscriptionCard";
import { CancelSubscriptionButton } from "@/components/subscription/CancelSubscriptionButton";
import { PassUsageSection } from "@/components/subscription/PassUsageSection";
import { ChevronRightIcon } from "@/components/icons";
import { getMySubscription, getSubscriptionPass } from "@/lib/api/subscription";
import { ApiError } from "@/lib/api/client";
import { useAuthStore } from "@/store/authStore";

export default function SubscriptionPage() {
  const router = useRouter();
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);
  const authLoading = useAuthStore((s) => s.isLoading);

  const [subscription, setSubscription] = useState(null);
  const [currentPass, setCurrentPass] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [loadFailed, setLoadFailed] = useState(false);

  const load = useCallback(async () => {
    setIsLoading(true);
    setLoadFailed(false);
    try {
      const sub = await getMySubscription();
      setSubscription(sub);
      if (sub?.id) {
        try {
          const pass = await getSubscriptionPass(sub.id);
          setCurrentPass(pass ?? null);
        } catch {
          setCurrentPass(null);
        }
      }
    } catch (err) {
      // 구독 이력이 없는 사용자는 404를 받는 것이 정상이므로 별도 에러로 취급하지 않습니다.
      if (err instanceof ApiError && err.status === 404) {
        setSubscription(null);
      } else {
        setLoadFailed(true);
      }
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    if (authLoading) return;
    if (!isAuthenticated) {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setIsLoading(false);
      return;
    }
    load();
  }, [authLoading, isAuthenticated, load]);

  const isActive = subscription?.status === "ACTIVE";

  return (
    <AppShell header={<PageHeader title="구독" />}>
      {!isAuthenticated && !authLoading && (
        <>
          <p className="text-[13px] text-ink-soft">
            구독하면 매달 클래스 수강권 3개가 제공돼요. 클래스 가격과 관계없이 수강권 1개로 원하는 클래스를
            신청할 수 있어요.
          </p>
          <PlanCard
            actionLabel="로그인하고 구독하기"
            onAction={() => router.push(`/login?from=${encodeURIComponent("/subscription")}`)}
          />
        </>
      )}

      {isAuthenticated && isLoading && <p className="py-3 text-center text-sm text-ink-muted">불러오는 중...</p>}

      {isAuthenticated && !isLoading && loadFailed && (
        <p className="rounded-lg bg-red-50 px-3.5 py-2.5 text-[13px] text-red-700">
          구독 정보를 불러오지 못했어요. 잠시 후 다시 시도해주세요.
        </p>
      )}

      {isAuthenticated && !isLoading && !loadFailed && isActive && (
        <>
          <CurrentSubscriptionCard subscription={subscription} passSummary={currentPass} />

          <Link
            href="/subscription/billing"
            className="flex items-center justify-between rounded-xl border border-border bg-white px-4 py-3.5"
          >
            <span className="text-[14px] font-semibold">결제수단 관리</span>
            <ChevronRightIcon size={18} className="text-ink-muted" />
          </Link>

          {currentPass && <PassUsageSection passId={currentPass.id} />}

          {!subscription.cancelAtPeriodEnd && (
            <CancelSubscriptionButton subscriptionId={subscription.id} onCancelled={load} />
          )}
        </>
      )}

      {isAuthenticated && !isLoading && !loadFailed && !isActive && (
        <>
          <p className="text-[13px] text-ink-soft">
            구독하면 매달 클래스 수강권 3개가 제공돼요. 클래스 가격과 관계없이 수강권 1개로 원하는 클래스를
            신청할 수 있어요.
          </p>
          <PlanCard actionLabel="구독 시작하기" onAction={() => router.push("/subscription/billing")} />
        </>
      )}
    </AppShell>
  );
}
