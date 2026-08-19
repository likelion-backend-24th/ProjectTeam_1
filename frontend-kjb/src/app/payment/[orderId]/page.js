"use client";

import { useCallback, useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { AppShell, PageHeader } from "@/components/AppShell";
import { RequireAuth } from "@/components/RequireAuth";
import { BackIcon } from "@/components/icons";
import { StatusPill } from "@/components/payment/StatusPill";
import { CancelPaymentButton } from "@/components/payment/CancelPaymentButton";
import { ORDER_STATUS_LABEL, PAYMENT_STATUS_LABEL } from "@/lib/constants/status";
import { getOrder } from "@/lib/api/order";
import { ApiError } from "@/lib/api/client";
import { formatCurrency, formatDateTime } from "@/utils/format";

// 결제 직후 랜딩 화면이자, 결제 내역에서 항목을 눌렀을 때의 상세 화면.
// 이 화면에 표시하는 상태는 전부 백엔드 응답(order/order.payment)을 그대로 따른다 —
// 프론트에서 결제 성공 여부를 별도로 판단하지 않는다.
function OrderDetailView() {
  const { orderId } = useParams();
  const [order, setOrder] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);

  const load = useCallback(
    async (signal) => {
      setIsLoading(true);
      setError(null);
      try {
        const data = await getOrder(orderId, signal);
        if (!signal?.aborted) setOrder(data);
      } catch (err) {
        if (!signal?.aborted) {
          setError(err instanceof ApiError ? err.message : "주문 정보를 불러오지 못했어요.");
        }
      } finally {
        if (!signal?.aborted) setIsLoading(false);
      }
    },
    [orderId],
  );

  useEffect(() => {
    const controller = new AbortController();
    // eslint-disable-next-line react-hooks/set-state-in-effect
    load(controller.signal);
    return () => controller.abort();
  }, [load]);

  if (isLoading) return <p className="py-3 text-center text-sm text-ink-muted">불러오는 중...</p>;
  if (error) return <p className="rounded-lg bg-red-50 px-3.5 py-2.5 text-[13px] text-red-700">{error}</p>;
  if (!order) return null;

  const payment = order.payment;
  const canShowCancel = order.orderType === "GENERAL" && order.orderStatus === "PAID" && payment;

  return (
    <>
      <div className="flex flex-col items-center gap-2 py-4 text-center">
        <span className="text-3xl">{order.orderStatus === "PAID" ? "✅" : order.orderStatus === "FAILED" ? "❌" : "⏳"}</span>
        <p className="text-[17px] font-extrabold">
          {order.orderStatus === "PAID"
            ? "결제가 완료됐어요"
            : order.orderStatus === "PENDING"
              ? "결제 확인 중이에요"
              : order.orderStatus === "FAILED"
                ? "결제에 실패했어요"
                : "결제가 취소됐어요"}
        </p>
      </div>

      <div className="flex flex-col gap-3 rounded-[18px] border border-border bg-white p-5">
        <div className="flex items-center justify-between">
          <span className="text-[14px] font-bold">{order.title ?? (order.orderType === "SUBSCRIPTION" ? "BASIC 구독" : "클래스 결제")}</span>
          <StatusPill status={order.orderStatus} label={ORDER_STATUS_LABEL[order.orderStatus]} />
        </div>
        <hr className="border-border" />
        <Row label="주문번호" value={order.merchantOrderId ?? order.id} />
        <Row label="결제일" value={formatDateTime(payment?.approvedAt ?? order.createdAt)} />
        <Row label="결제 방법" value={payment?.payMethod ?? "-"} />
        <Row label="결제 금액" value={formatCurrency(order.amount)} strong />
        {payment?.status && (
          <Row label="결제 상태" value={PAYMENT_STATUS_LABEL[payment.status] ?? payment.status} />
        )}
      </div>

      {canShowCancel && <CancelPaymentButton payment={payment} onCancelled={() => load()} />}
    </>
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

export default function OrderDetailPage() {
  const router = useRouter();

  return (
    <RequireAuth>
      <AppShell
        header={
          <PageHeader
            title="주문 상세"
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
        <OrderDetailView />
      </AppShell>
    </RequireAuth>
  );
}
