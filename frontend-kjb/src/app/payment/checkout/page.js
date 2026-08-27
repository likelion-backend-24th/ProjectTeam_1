"use client";

import { Suspense, useCallback, useEffect, useRef, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { AppShell, PageHeader } from "@/components/AppShell";
import { RequireAuth } from "@/components/RequireAuth";
import { BackIcon } from "@/components/icons";
import { getOrder } from "@/lib/api/order";
import { verifyPayment } from "@/lib/api/payment";
import { requestPayment } from "@/lib/portone/client";
import { ApiError } from "@/lib/api/client";
import { useToastStore } from "@/store/toastStore";
import { useAuthStore } from "@/store/authStore";
import { formatCurrency, formatDateTime } from "@/utils/format";

// 결제 확인 화면. 여기 표시되는 금액과 주문 정보는 전부 백엔드가 이미 생성해둔
// 주문(order)을 그대로 보여줄 뿐이며, 프론트에서 금액을 계산하거나 바꾸지 않는다.
// PortOne 결제창 호출은 requestPayment() 한 곳으로만 분리되어 있다.
//
// pendingRef: 결제 대기(PENDING) 중에 이 화면을 벗어나려 하면 한 번 경고하기 위한 플래그.
// 신청 시점에 정원을 이미 점유한 상태(PENDING)라 결제를 안 하고 나가면 그 자리가 방치되므로,
// 상위(CheckoutPage)의 뒤로가기 버튼에서도 같은 값을 읽을 수 있도록 ref로 끌어올려 공유한다.
function CheckoutView({ pendingRef }) {
  const router = useRouter();
  const searchParams = useSearchParams();
  const orderId = searchParams.get("orderId");
  const showToast = useToastStore((s) => s.showToast);
  const profile = useAuthStore((s) => s.profile);

  const [order, setOrder] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);
  const [isPaying, setIsPaying] = useState(false);

  useEffect(() => {
    pendingRef.current = order?.orderStatus === "PENDING";
  }, [order, pendingRef]);

  useEffect(() => {
    function handleBeforeUnload(e) {
      if (!pendingRef.current) return;
      e.preventDefault();
      e.returnValue = "";
    }
    window.addEventListener("beforeunload", handleBeforeUnload);
    return () => window.removeEventListener("beforeunload", handleBeforeUnload);
  }, [pendingRef]);

  const load = useCallback(
    async (signal) => {
      if (!orderId) {
        setError("결제할 주문 정보가 없어요.");
        setIsLoading(false);
        return;
      }
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

  async function handlePay() {
    setIsPaying(true);
    try {
      const result = await requestPayment({ order, profile });
      // PortOne 결제창 결과만으로는 성공을 확정하지 않고, 반드시 백엔드 검증을 거친다.
      // PaymentVerifyRequestDto는 merchantOrderId 필드를 요구한다(order.id가 아님) — 주문의 PK가 아니라
      // 백엔드가 생성한 주문번호 문자열로 조회하기 때문.
      await verifyPayment({ merchantOrderId: order.merchantOrderId, paymentId: result.paymentId });
      router.replace(`/payment/${order.id}`);
    } catch (err) {
      showToast(
        err instanceof ApiError
          ? err.message
          : err?.message?.startsWith("NOT_IMPLEMENTED")
            ? "결제 기능은 PortOne 연동 확정 후 사용할 수 있어요."
            : "결제에 실패했어요. 다시 시도해주세요.",
        "error",
      );
    } finally {
      setIsPaying(false);
    }
  }

  if (isLoading) return <p className="py-3 text-center text-sm text-ink-muted">불러오는 중...</p>;
  if (error) return <p className="rounded-lg bg-red-50 px-3.5 py-2.5 text-[13px] text-red-700">{error}</p>;
  if (!order) return null;

  return (
    <>
      <div className="flex flex-col gap-3 rounded-[18px] border border-border bg-white p-5">
        <p className="text-[15px] font-extrabold">{order.title ?? "클래스 결제"}</p>
        {order.scheduledAt && <Row label="클래스 일정" value={formatDateTime(order.scheduledAt)} />}
        <hr className="border-border" />
        <Row label="최종 결제 금액" value={formatCurrency(order.amount)} strong />
        <Row label="결제 방법" value="카드 (PortOne)" />
      </div>

      <button
        type="button"
        onClick={handlePay}
        disabled={isPaying}
        className="h-[50px] w-full rounded-xl bg-primary text-[15px] font-semibold text-white disabled:opacity-50"
      >
        {isPaying ? "결제 진행 중..." : `${formatCurrency(order.amount)} 결제하기`}
      </button>
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

export default function CheckoutPage() {
  const router = useRouter();
  // CheckoutView 내부의 order 상태를 리렌더 없이 읽기 위한 ref (위 pendingRef 설명 참고)
  const pendingRef = useRef(false);

  function handleBack() {
    if (
      pendingRef.current &&
      !window.confirm("결제를 완료하지 않고 나가면 신청이 취소될 수 있어요. 나가시겠어요?")
    ) {
      return;
    }
    router.back();
  }

  return (
    <RequireAuth>
      <AppShell
        header={
          <PageHeader
            title="결제 확인"
            left={
              <button
                type="button"
                className="flex h-8 w-8 items-center justify-center rounded-lg"
                onClick={handleBack}
                aria-label="뒤로가기"
              >
                <BackIcon />
              </button>
            }
          />
        }
      >
        <Suspense fallback={<p className="py-3 text-center text-sm text-ink-muted">불러오는 중...</p>}>
          <CheckoutView pendingRef={pendingRef} />
        </Suspense>
      </AppShell>
    </RequireAuth>
  );
}
