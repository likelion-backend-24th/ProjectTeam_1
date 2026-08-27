"use client";

import { useCallback, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { AppShell, PageHeader } from "@/components/AppShell";
import { RequireAuth } from "@/components/RequireAuth";
import { BackIcon } from "@/components/icons";
import { useToastStore } from "@/store/toastStore";
import { ApiError } from "@/lib/api/client";
import { getAdminSettlements, payoutSettlement } from "@/lib/api/settlement";

const FILTERS = [
  { label: "전체", value: "ALL" },
  { label: "정산 대기", value: "PENDING" },
  { label: "정산 완료", value: "COMPLETED" },
];

function AdminSettlementView() {
  const showToast = useToastStore((s) => s.showToast);
  const [settlements, setSettlements] = useState([]);
  const [filter, setFilter] = useState("ALL");
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);
  const [processingId, setProcessingId] = useState(null);

  const load = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await getAdminSettlements();
      setSettlements(data ?? []);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "정산 내역을 불러오지 못했어요.");
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  // 정산 지급 완료 처리 핸들러
  async function handlePayoutSettlement(id) {
    if (!window.confirm("해당 건을 정산 지급 완료 처리하시겠어요?")) return;

    setProcessingId(id);
    try {
      await payoutSettlement(id);
      showToast("정산 지급 완료 처리되었습니다.");
      // 프론트엔드 상태도 COMPLETED로 업데이트
      setSettlements((prev) =>
        prev.map((item) => (item.id === id ? { ...item, status: "COMPLETED" } : item))
      );
    } catch (err) {
      showToast(err instanceof ApiError ? err.message : "정산 처리에 실패했어요.", "error");
    } finally {
      setProcessingId(null);
    }
  }

  const filtered = filter === "ALL" ? settlements : settlements.filter((item) => item.status === filter);

  const formatDate = (dateString) => {
    if (!dateString) return "-";
    const date = new Date(dateString);
    return isNaN(date.getTime()) 
      ? dateString 
      : date.toLocaleDateString("ko-KR", { year: "numeric", month: "2-digit", day: "2-digit", hour: "2-digit", minute: "2-digit" });
  };

  return (
    <div className="flex flex-col gap-4">
      {/* 상단 필터 탭 */}
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
        <p className="py-12 text-center text-sm text-ink-muted">조회된 정산 내역이 없어요.</p>
      )}

      {/* 정산 목록 카드 */}
      <ul className="flex flex-col gap-3">
        {filtered.map((item) => (
          <li key={item.id} className="flex flex-col gap-3 rounded-2xl border border-border bg-white p-4 shadow-sm">
            <div className="flex items-center justify-between">
              <span className="text-[12px] font-semibold text-ink-muted">
                정산번호 #{item.id}
              </span>
              <span
                className={`rounded-full px-2.5 py-0.5 text-[11px] font-semibold ${
                  item.status === "COMPLETED"
                    ? "bg-green-100 text-green-800"
                    : item.status === "CANCELLED"
                    ? "bg-red-100 text-red-800"
                    : "bg-yellow-100 text-yellow-800"
                }`}
              >
                {item.status === "COMPLETED" ? "정산 완료" : item.status === "CANCELLED" ? "취소됨" : "정산 대기"}
              </span>
            </div>

            <div className="flex flex-col gap-1">
              <span className="text-[15px] font-bold text-ink">
                {item.className || "-"}
              </span>
              <span className="text-[12px] text-ink-muted">
                생성일시: {formatDate(item.createdAt)}
              </span>
            </div>

            <div className="mt-1 flex items-center justify-between border-t border-border pt-3 text-[13px]">
              <div className="flex flex-col">
                <span className="text-[11px] text-ink-muted">결제 금액 ({item.settlementRate ? `${item.settlementRate}%` : '-'})</span>
                <span className="font-semibold text-ink">
                  {item.paymentAmount?.toLocaleString()}원
                </span>
              </div>
              <div className="flex flex-col text-right">
                <span className="text-[11px] text-ink-muted">정산 금액</span>
                <span className="text-[15px] font-bold text-primary">
                  {item.settlementAmount?.toLocaleString()}원
                </span>
              </div>
            </div>

            {/* PENDING 상태일 때 보여지는 정산 지급 완료 버튼 */}
            {item.status === "PENDING" && (
              <button
                type="button"
                onClick={() => handlePayoutSettlement(item.id)}
                disabled={processingId === item.id}
                className="mt-1 h-[38px] w-full rounded-xl bg-primary text-[13px] font-semibold text-white disabled:opacity-50"
              >
                {processingId === item.id ? "처리 중..." : "정산 지급 완료 처리"}
              </button>
            )}
          </li>
        ))}
      </ul>
    </div>
  );
}

export default function AdminSettlementPage() {
  const router = useRouter();

  return (
    <RequireAuth>
      <AppShell
        header={
          <PageHeader
            title="전체 정산 관리"
            left={
              <button
                type="button"
                className="flex h-8 w-8 items-center justify-center rounded-lg cursor-pointer"
                onClick={() => router.back()}
                aria-label="뒤로가기"
              >
                <BackIcon />
              </button>
            }
          />
        }
      >
        <AdminSettlementView />
      </AppShell>
    </RequireAuth>
  );
}