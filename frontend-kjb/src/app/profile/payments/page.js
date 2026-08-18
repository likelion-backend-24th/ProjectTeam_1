"use client";

import { useCallback, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { AppShell, PageHeader } from "@/components/AppShell";
import { RequireAuth } from "@/components/RequireAuth";
import { BackIcon } from "@/components/icons";
import { PaymentHistoryItem } from "@/components/payment/PaymentHistoryItem";
import { getMyOrders } from "@/lib/api/order";
import { ApiError } from "@/lib/api/client";

const FILTERS = [
  { label: "전체", value: "ALL" },
  { label: "일반 결제", value: "GENERAL" },
  { label: "구독 결제", value: "SUBSCRIPTION" },
];

function PaymentHistoryView() {
  const [orders, setOrders] = useState([]);
  const [filter, setFilter] = useState("ALL");
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);

  const load = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const res = await getMyOrders({ page: 0, size: 50 });
      setOrders(res?.content ?? res ?? []);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "결제 내역을 불러오지 못했어요.");
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    load();
  }, [load]);

  const filtered = filter === "ALL" ? orders : orders.filter((o) => o.orderType === filter);

  return (
    <>
      <div className="flex flex-wrap items-center gap-2">
        {FILTERS.map((f) => (
          <button
            key={f.value}
            type="button"
            onClick={() => setFilter(f.value)}
            className={`shrink-0 rounded-full px-3.5 py-2 text-[13px] font-semibold ${
              filter === f.value ? "bg-primary text-white" : "bg-surface text-ink-soft"
            }`}
          >
            {f.label}
          </button>
        ))}
      </div>

      {isLoading && <p className="py-3 text-center text-sm text-ink-muted">불러오는 중...</p>}
      {error && <p className="rounded-lg bg-red-50 px-3.5 py-2.5 text-[13px] text-red-700">{error}</p>}

      {!isLoading && !error && filtered.length === 0 && (
        <p className="py-15 text-center text-sm text-ink-muted">결제 내역이 없어요.</p>
      )}

      <ul className="flex flex-col gap-2.5">
        {filtered.map((order) => (
          <li key={order.id}>
            <PaymentHistoryItem order={order} />
          </li>
        ))}
      </ul>
    </>
  );
}

export default function PaymentHistoryPage() {
  const router = useRouter();

  return (
    <RequireAuth>
      <AppShell
        header={
          <PageHeader
            title="결제 내역"
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
        <PaymentHistoryView />
      </AppShell>
    </RequireAuth>
  );
}
