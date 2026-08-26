"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { AppShell, PageHeader } from "@/components/AppShell";
import { RequireAuth } from "@/components/RequireAuth";
import { BackIcon, CalendarIcon, ChevronRightIcon, PencilIcon, SproutIcon } from "@/components/icons";
import { useAuthStore } from "@/store/authStore";
import { getMyFarms, resolveFarmImageUrl } from "@/lib/api/farm";
import { getHostReservationSummary } from "@/lib/api/host";
import { getMyClasses } from "@/lib/api/onedayclass";
import { formatDate } from "@/utils/format";

const TABS = [
  { key: "ALL", label: "전체" },
  { key: "FARM_AVAILABLE", label: "임대가능" },
  { key: "FARM_RENTED", label: "임대중" },
  { key: "FARM_ENDED", label: "임대종료" },
  { key: "CLASS_ONGOING", label: "모집중" },
  { key: "CLASS_ENDED", label: "모집종료" },
];

// 임대중인 밭이 계약 개월수를 다 채웠는지(자동 임대종료) 판단
function isRentalEnded(farm) {
  if (farm.farmStatus !== "RENTED" || !farm.rentalStartedAt) return false;
  const start = new Date(farm.rentalStartedAt);
  const end = new Date(start);
  end.setMonth(end.getMonth() + (farm.rentalMonths ?? 0));
  return new Date() >= end;
}

function HostFarmsView() {
  const router = useRouter();
  const isHost = useAuthStore((s) => s.isHost);
  const [farms, setFarms] = useState([]);
  const [thisMonthCount, setThisMonthCount] = useState(0);
  const [myClasses, setMyClasses] = useState([]);
  const [activeTab, setActiveTab] = useState("ALL");
  const [isAddMenuOpen, setIsAddMenuOpen] = useState(false);
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

        getMyClasses()
          .then((classes) => {
            if (!ignore) setMyClasses(classes);
          })
          .catch(() => {});
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

  const items = [
    ...farms.map((farm) => ({ type: "FARM", data: farm })),
    ...myClasses.map((cls) => ({ type: "CLASS", data: cls })),
  ];

  const filteredItems = items.filter(({ type, data }) => {
    if (activeTab === "ALL") return true;
    if (type === "FARM") {
      if (activeTab === "FARM_AVAILABLE") return data.farmStatus === "AVAILABLE";
      if (activeTab === "FARM_RENTED") return data.farmStatus === "RENTED" && !isRentalEnded(data);
      if (activeTab === "FARM_ENDED") return isRentalEnded(data);
      return false;
    }
    const isEnded = new Date(data.date) < new Date();
    if (activeTab === "CLASS_ONGOING") return !isEnded;
    if (activeTab === "CLASS_ENDED") return isEnded;
    return false;
  });

  const operatingCount = farms.filter((f) => f.farmStatus === "RENTED").length;
  const inactiveCount = farms.filter((f) => f.farmStatus === "AVAILABLE").length;
  const openClassCount = myClasses.filter((c) => new Date(c.date) >= new Date()).length;
  const endedClassCount = myClasses.filter((c) => new Date(c.date) < new Date()).length;
  return (
    <AppShell
      header={
        <PageHeader
          title="호스트 관리"
          left={<button type="button" onClick={() => router.back()}><BackIcon /></button>}
        />
      }
    >
      <div className="flex flex-col gap-4 rounded-2xl bg-gradient-to-br from-emerald-700 to-emerald-500 px-4 py-5 text-white">
        <div className="flex items-center justify-between gap-4">
          <div className="flex flex-col gap-2">
            <div className="flex items-center gap-2">
              <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-white/15">
                <SproutIcon size={18} />
              </span>
              <span className="text-[14px] font-semibold text-emerald-50">올린 땅 수</span>
            </div>
            <span className="text-3xl font-bold">{farms.length}개 🏡</span>
          </div>
          <div className="flex flex-col items-end gap-2">
            <div className="flex items-center gap-2">
              <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-white/15">
                <CalendarIcon size={18} />
              </span>
              <span className="text-[14px] font-semibold text-emerald-50">총 클래스</span>
            </div>
            <span className="text-3xl font-bold">{myClasses.length}개 📅</span>
          </div>
        </div>

        <div className="flex items-center justify-between gap-4">
          <div className="flex items-center gap-4">
            <div className="flex flex-col gap-2">
              <span className="w-fit rounded-full bg-white/15 px-3 py-1 text-[12px] font-semibold text-white">운영 중</span>
              <span className="text-lg font-bold">{operatingCount}개 🌱</span>
            </div>
            <div className="h-10 w-px bg-white/20" />
            <div className="flex flex-col gap-2">
              <span className="w-fit rounded-full bg-white/10 px-3 py-1 text-[12px] font-semibold text-emerald-50">미운영</span>
              <span className="text-lg font-bold">{inactiveCount}개 🍃</span>
            </div>
          </div>
          <div className="flex items-center gap-4">
            <div className="flex flex-col items-end gap-2">
              <span className="w-fit rounded-full bg-white/15 px-3 py-1 text-[12px] font-semibold text-white">진행중</span>
              <span className="text-lg font-bold">{openClassCount}개 📖</span>
            </div>
            <div className="h-10 w-px bg-white/20" />
            <div className="flex flex-col items-end gap-2">
              <span className="w-fit rounded-full bg-white/10 px-3 py-1 text-[12px] font-semibold text-emerald-50">종료</span>
              <span className="text-lg font-bold">{endedClassCount}개 ✅</span>
            </div>
          </div>
        </div>

        <div className="flex items-center border-t border-white/20 pt-3">
          <Link href="/host/settlements" className="text-[13px] font-semibold text-emerald-50">
            정산 내역 보기 &gt;
          </Link>
        </div>
      </div>

      <div className="scrollbar-hide flex items-center gap-2 overflow-x-auto">
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

      {!isLoading && !error && filteredItems.length === 0 && (
        <div className="py-15 text-center text-sm text-ink-muted">
          <div className="mx-auto mb-3.5 flex h-14 w-14 items-center justify-center rounded-full bg-surface text-2xl">
            🌱
          </div>
          {activeTab === "ALL" ? "아직 등록한 땅이나 클래스가 없어요." : "해당하는 항목이 없어요."}
        </div>
      )}

      <ul className="flex flex-col gap-3">
        {filteredItems.map(({ type, data }) =>
          type === "FARM" ? (
            <li key={`farm-${data.id}`} className="flex flex-col gap-3 rounded-2xl border border-border bg-white p-3.5">
              <div className="flex items-center justify-between gap-3">
                <div className="flex gap-3">
                 {data.thumbnailUrl ? (
                    <img
                      src={resolveFarmImageUrl(data.thumbnailUrl)}
                      alt={data.title}
                      className="h-20 w-20 shrink-0 rounded-xl object-cover"
                    />
                  ) : (
                    <div className="h-20 w-20 shrink-0 rounded-xl bg-gradient-to-br from-emerald-200 to-emerald-100" />
                  )}
                  <div className="flex flex-1 flex-col justify-center gap-1 overflow-hidden">
                    <p className="truncate text-[15px] font-bold text-ink">{data.title}</p>
                    <p className="truncate text-[13px] text-ink-muted">{data.location}</p>
                    <span
                      className={`w-fit rounded-full px-2 py-0.5 text-[11px] font-semibold ${
                        data.farmStatus === "RENTED" ? "bg-free-soft text-free" : "bg-surface text-ink-soft"
                      }`}
                    >
                      {data.farmStatus === "RENTED" ? "임대중" : "임대 가능"}
                    </span>
                  </div>
                </div>
                <Link href={`/farms/${data.id}`} className="flex shrink-0 items-center gap-1 text-[13px] font-semibold text-free">
                  관리하기
                  <ChevronRightIcon size={16} className="text-free" />
                </Link>
              </div>

              {data.farmStatus === "RENTED" && data.tenantNickname && (
                <div className="flex flex-col gap-0.5 border-t border-border pt-3">
                  <span className="text-[14px] font-bold text-ink">{data.tenantNickname}</span>
                  <span className="text-[12px] text-ink-muted">
                    일반결제 · {formatDate(data.rentalStartedAt)}
                  </span>
                </div>
              )}
            </li>
          ) : (
            (() => {
              const isEnded = new Date(data.date) < new Date();
              return (
                <li key={`class-${data.id}`} className="flex flex-col gap-3 rounded-2xl border border-border bg-white p-3.5">
                  <div className="flex items-center justify-between gap-3">
                    <div className="flex gap-3">
                      <div className="flex h-20 w-20 shrink-0 items-center justify-center rounded-xl bg-gradient-to-br from-emerald-200 to-emerald-100">
                        <CalendarIcon size={28} className="text-free" />
                      </div>
                      <div className="flex flex-1 flex-col justify-center gap-1 overflow-hidden">
                        <p className="truncate text-[15px] font-bold text-ink">{data.title}</p>
                        <p className="truncate text-[13px] text-ink-muted">{formatDate(data.date)} · {data.location}</p>
                        <span
                          className={`w-fit rounded-full px-2 py-0.5 text-[11px] font-semibold ${
                            !isEnded ? "bg-free-soft text-free" : "bg-surface text-ink-soft"
                          }`}
                        >
                          {isEnded ? "종료" : "진행중"} · 신청 {data.enrolledCount}/{data.capacity}
                        </span>
                      </div>
                    </div>
                    <Link href={`/class/${data.id}`} className="flex shrink-0 items-center gap-1 text-[13px] font-semibold text-free">
                      관리하기
                      <ChevronRightIcon size={16} className="text-free" />
                    </Link>
                  </div>
                </li>
              );
            })()
          ),
        )}
      </ul>

      <div className="absolute right-5 bottom-[calc(60px_+_20px)] flex flex-col items-end gap-3">
        {isAddMenuOpen && (
          <>
            <Link
              href="/class/write"
              className="flex items-center gap-2.5 rounded-full bg-white py-1 pr-1 pl-4 shadow-[0_6px_16px_rgba(0,0,0,0.2)] ring-1 ring-border"
            >
              <span className="text-[13px] font-semibold text-ink">클래스 등록</span>
              <span className="flex h-9 w-9 items-center justify-center rounded-full bg-free-soft text-free">
                <CalendarIcon size={16} />
              </span>
            </Link>
            <Link
              href="/farms/new"
              className="flex items-center gap-2.5 rounded-full bg-white py-1 pr-1 pl-4 shadow-[0_6px_16px_rgba(0,0,0,0.2)] ring-1 ring-border"
            >
              <span className="text-[13px] font-semibold text-ink">땅 등록</span>
              <span className="flex h-9 w-9 items-center justify-center rounded-full bg-free-soft text-free">
                <SproutIcon size={16} />
              </span>
            </Link>
          </>
        )}
        <button
          type="button"
          onClick={() => setIsAddMenuOpen((prev) => !prev)}
          aria-label="등록"
          aria-expanded={isAddMenuOpen}
          className="flex h-[52px] w-[52px] items-center justify-center rounded-full bg-free text-white shadow-[0_6px_16px_rgba(0,0,0,0.2)] transition-transform"
        >
          <PencilIcon size={22} className={isAddMenuOpen ? "rotate-45 transition-transform" : "transition-transform"} />
        </button>
      </div>
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