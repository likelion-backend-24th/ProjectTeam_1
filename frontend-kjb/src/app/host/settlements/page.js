"use client";

import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import { AppShell, PageHeader } from "@/components/AppShell";
import { getHostSettlements } from "@/lib/api/settlement";
import { BackIcon } from "@/components/icons";
import { SETTLEMENT_STATUS_LABEL } from "@/lib/constants/status";

export default function HostSettlementPage() {
  const router = useRouter();
  const [settlements, setSettlements] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchSettlements = async () => {
      try {
        const data = await getHostSettlements();
        setSettlements(data);
      } catch (err) {
        setError(err.message || "정산 내역을 불러오는데 실패했습니다.");
      } finally {
        setLoading(false);
      }
    };

    fetchSettlements();
  }, []);

  const formatDate = (dateString) => {
    if (!dateString) return "-";
    const date = new Date(dateString);
    return isNaN(date.getTime()) 
      ? dateString 
      : date.toLocaleDateString("ko-KR", { year: "numeric", month: "2-digit", day: "2-digit", hour: "2-digit", minute: "2-digit" });
  };

  return (
    <AppShell
      header={
        <PageHeader
          title="내 정산 내역"
          left={
            <button type="button" onClick={() => router.back()} className="cursor-pointer">
              <BackIcon size={22} />
            </button>
          }
        />
      }
    >
      {loading && (
        <div className="flex justify-center items-center py-12">
          <p className="text-sm text-ink-muted">정산 내역을 불러오는 중...</p>
        </div>
      )}

      {error && (
        <div className="rounded-xl bg-red-50 px-4 py-3 text-sm text-red-700">
          에러 발생: {error}
        </div>
      )}

      {!loading && !error && (
        <div className="flex flex-col gap-3">
          {settlements.length === 0 ? (
            <div className="flex flex-col items-center justify-center rounded-2xl bg-surface px-4 py-12 text-center text-sm text-ink-muted">
              조회된 정산 내역이 없습니다.
            </div>
          ) : (
            settlements.map((item) => (
              <div
                key={item.id}
                className="flex flex-col gap-3 rounded-2xl border border-border bg-white p-4 shadow-sm"
              >
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
                    {SETTLEMENT_STATUS_LABEL[item.status] ?? item.status}
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
              </div>
            ))
          )}
        </div>
      )}
    </AppShell>
  );
}