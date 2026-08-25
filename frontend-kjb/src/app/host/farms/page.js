"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { AppShell, PageHeader } from "@/components/AppShell";
import { RequireAuth } from "@/components/RequireAuth";
import { BackIcon, ChevronRightIcon, PencilIcon } from "@/components/icons";
import { useAuthStore } from "@/store/authStore";
import { getMyFarms, resolveFarmImageUrl } from "@/lib/api/farm";
import { getHostReservationSummary } from "@/lib/api/host";

const TABS = [
  { key: "ALL", label: "전체" },
  { key: "OPERATING", label: "운영중" },
  { key: "CLOSED", label: "종료" },
];

function HostFarmsView() {
  const router = useRouter();
  const isHost = useAuthStore((s) => s.isHost);
  const [farms, setFarms] = useState([]);
  const [thisMonthCount, setThisMonthCount] = useState(0);
  const [activeTab, setActiveTab] = useState("ALL");
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!isHost) return;
    let ignore = false;
    (async () => {
      setIsLoading(true);
      setError(null);
      try {
        const [farmList, summary] = await Promise.all([getMyFarms(), getHostReservationSummary()]);
        if (ignore) return;
        setFarms(farmList);
        setThisMonthCount(summary.thisMonthCount);
       } catch (err) {
        if (!ignore) setError(err?.code ? err.message : "내 땅 목록을 불러오지 못했어요.");
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
            title="호스트 관리"
            left={<button type="button" onClick={() => router.back()}><BackIcon /></button>}
          />
        }
      >
        <div className="flex flex-1 flex-col items-center justify-center gap-2 text-center text-sm text-ink-muted">
          <p>호스트만 이용할 수 있는 페이지예요.</p>
          <Link href="/mypage/host-apply" className="font-semibold text-free">
            호스트 신청하러 가기
          </Link>
        </div>
      </AppShell>
    );
  }

  const filteredFarms = farms.filter((farm) => {
    if (activeTab === "ALL") return true;
    if (activeTab === "OPERATING") return farm.farmStatus === "RENTED";
    return false; // 종료 상태 개념이 없어 항상 비어있음
  });

  return (
    <AppShell
      header={
        <PageHeader
          title="호스트 관리"
          left={<button type="button" onClick={() => router.back()}><BackIcon /></button>}
        />
      }
    >
      <div className="flex flex-col gap-2 rounded-2xl bg-gradient-to-br from-emerald-700 to-emerald-500 px-4 py-5 text-white">
        <span className="text-[13px] font-medium text-emerald-50">올린 땅 수</span>
        <span className="text-2xl font-bold">{farms.length}개 🏡</span>
        <div className="mt-1 flex items-center justify-between">
          <Link href="/host/settlements" className="text-[13px] font-semibold text-emerald-50">
            정산 내역 보기 &gt;
          </Link>
        </div>
      </div>

      <div className="flex items-center gap-2">
        {TABS.map((tab) => (
          <button
            key={tab.key}
            type="button"
            onClick={() => setActiveTab(tab.key)}
            className={`shrink-0 rounded-full px-3.5 py-2 text-[13px] font-semibold ${
              activeTab === tab.key ? "bg-free text-white" : "bg-surface text-ink-soft"
            }`}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {error && <p className="rounded-lg bg-red-50 px-3.5 py-2.5 text-[13px] text-red-700">{error}</p>}

      {isLoading && <p className="py-10 text-center text-sm text-ink-muted">불러오는 중...</p>}

      {!isLoading && !error && filteredFarms.length === 0 && (
        <div className="py-15 text-center text-sm text-ink-muted">
          <div className="mx-auto mb-3.5 flex h-14 w-14 items-center justify-center rounded-full bg-surface text-2xl">
            🌱
          </div>
          {activeTab === "ALL" ? "아직 등록한 땅이 없어요." : "해당하는 땅이 없어요."}
        </div>
      )}

      <ul className="flex flex-col gap-3">
        {filteredFarms.map((farm) => (
           <li key={farm.id} className="flex items-center justify-between gap-3 rounded-2xl border border-border bg-white p-3.5">
            <div className="flex gap-3">
             {farm.thumbnailUrl ? (
                <img
                  src={resolveFarmImageUrl(farm.thumbnailUrl)}
                  alt={farm.title}
                  className="h-20 w-20 shrink-0 rounded-xl object-cover"
                />
              ) : (
                <div className="h-20 w-20 shrink-0 rounded-xl bg-gradient-to-br from-emerald-200 to-emerald-100" />
              )}
              <div className="flex flex-1 flex-col justify-center gap-1 overflow-hidden">
                <p className="truncate text-[15px] font-bold text-ink">{farm.title}</p>
                <p className="truncate text-[13px] text-ink-muted">{farm.location}</p>
                <span
                  className={`w-fit rounded-full px-2 py-0.5 text-[11px] font-semibold ${
                    farm.farmStatus === "RENTED" ? "bg-free-soft text-free" : "bg-surface text-ink-soft"
                  }`}
                >
                  {farm.farmStatus === "RENTED" ? "임대중" : "임대 가능"}
                </span>
              </div>
            </div>
            <Link href={`/farms/${farm.id}`} className="flex shrink-0 items-center gap-1 text-[13px] font-semibold text-free">
              관리하기
              <ChevronRightIcon size={16} className="text-free" />
            </Link>
          </li>
        ))}
      </ul>

      <Link
        href="/farms/new"
        aria-label="땅 등록"
        className="absolute right-5 bottom-[calc(60px_+_20px)] flex h-[52px] w-[52px] items-center justify-center rounded-full bg-free text-white shadow-[0_6px_16px_rgba(0,0,0,0.2)]"
      >
        <PencilIcon size={22} />
      </Link>
    </AppShell>
  );
}

export default function HostFarmsPage() {
  return (
    <RequireAuth>
      <HostFarmsView />
    </RequireAuth>
  );
}