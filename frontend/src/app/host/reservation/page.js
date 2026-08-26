"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { AppShell, PageHeader } from "@/components/AppShell";
import { RequireAuth } from "@/components/RequireAuth";
import { BackIcon } from "@/components/icons";
import { useAuthStore } from "@/store/authStore";
import { getHostReservationSummary } from "@/lib/api/host";
import { formatDateWithWeekday } from "@/utils/format";

const STATUS_LABEL = {
  REQUESTED: "신청됨",
  CONFIRMED: "예약 확정",
  CANCELLED: "취소됨",
};

const STATUS_STYLE = {
  REQUESTED: "bg-class-soft text-class",
  CONFIRMED: "bg-free-soft text-free",
  CANCELLED: "bg-surface text-ink-muted",
};

function HostReservationsView() {
  const router = useRouter();
  const isHost = useAuthStore((s) => s.isHost);
  const [summary, setSummary] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!isHost) return;
    let ignore = false;
    (async () => {
      setIsLoading(true);
      setError(null);
      try {
        const data = await getHostReservationSummary();
        if (!ignore) setSummary(data);
     } catch (err) {
        if (!ignore) setError(err?.code ? err.message : "예약 현황을 불러오지 못했어요.");
      } finally {
        if (!ignore) setIsLoading(false);
      }
    })();
    return () => {
      ignore = true;
    };
  }, [isHost]);

  if (!isHost) {
    return (
      <AppShell
        header={
          <PageHeader
            title="예약 현황"
            left={<button type="button" onClick={() => router.back()}><BackIcon /></button>}
          />
        }
      >
        <div className="flex flex-1 items-center justify-center text-sm text-ink-muted">
          호스트만 이용할 수 있는 페이지예요.
        </div>
      </AppShell>
    );
  }

  const stats = [
    { label: "예약 확정", value: summary?.confirmedCount },
    { label: "오늘 입주", value: summary?.todayCheckinCount },
    { label: "이용 중", value: summary?.inUseCount },
    { label: "이번 달 예약", value: summary?.thisMonthCount },
  ];

  return (
    <AppShell
      header={
        <PageHeader
          title="예약 현황"
          left={<button type="button" onClick={() => router.back()}><BackIcon /></button>}
        />
      }
    >
      {error && <p className="rounded-lg bg-red-50 px-3.5 py-2.5 text-[13px] text-red-700">{error}</p>}

      {isLoading && <p className="py-10 text-center text-sm text-ink-muted">불러오는 중...</p>}

      {!isLoading && (
        <>
          <div className="grid grid-cols-4 gap-2">
            {stats.map((s) => (
              <div key={s.label} className="flex flex-col items-center gap-1 rounded-2xl bg-surface px-2 py-3.5">
                <span className="text-lg font-bold text-ink">{s.value ?? 0}</span>
                <span className="text-center text-[11px] text-ink-muted">{s.label}</span>
              </div>
            ))}
          </div>

          <p className="text-[13px] font-semibold text-ink-soft">예약 목록 (최신순)</p>

          {!summary?.rentals || summary.rentals.length === 0 ? (
            <div className="py-15 text-center text-sm text-ink-muted">
              <div className="mx-auto mb-3.5 flex h-14 w-14 items-center justify-center rounded-full bg-surface text-2xl">
                📋
              </div>
              아직 예약 내역이 없어요.
            </div>
          ) : (
            <ul className="flex flex-col gap-2.5">
              {summary.rentals.map((rental) => (
                <li key={rental.id} className="flex flex-col gap-1.5 rounded-2xl border border-border bg-white p-3.5">
                  <div className="flex items-center justify-between gap-2">
                    <p className="truncate text-[14px] font-bold text-ink">{rental.farmTitle}</p>
                    <span
                      className={`shrink-0 rounded-full px-2 py-0.5 text-[11px] font-semibold ${
                        STATUS_STYLE[rental.rentalStatus] ?? "bg-surface text-ink-soft"
                      }`}
                    >
                      {STATUS_LABEL[rental.rentalStatus] ?? rental.rentalStatus}
                    </span>
                  </div>
                  <p className="text-[13px] text-ink-soft">{rental.userNickname}님</p>
                  <p className="text-[12px] text-ink-muted">입주 예정일 {formatDateWithWeekday(rental.rentalStart)}</p>
                </li>
              ))}
            </ul>
          )}
        </>
      )}
    </AppShell>
  );
}

export default function HostReservationsPage() {
  return (
    <RequireAuth>
      <HostReservationsView />
    </RequireAuth>
  );
}