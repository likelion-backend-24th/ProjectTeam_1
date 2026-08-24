"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { AppShell, PageHeader } from "@/components/AppShell";
import { RequireAuth } from "@/components/RequireAuth";
import { ChevronRightIcon, SettingsIcon, BookIcon } from "@/components/icons";import { useAuthStore } from "@/store/authStore";
import { useToastStore } from "@/store/toastStore";
import { getMyEnrollments } from "@/lib/api/enrollment";
import { formatDateWithWeekday, formatTime } from "@/utils/format";

// 예약 확정 더미 데이터 (밭 임대 목록 조회 API 생기면 실제 데이터로 교체 예정)
const dummyReservations = [
  { id: 1, thumbnailUrl: null, farmTitle: "임대 타이틀", location: "서울시 청담동", rentalStart: "9.22 (일) 15:00", rentalEnd: "9.23 (월) 11:00", isMyFarm: false },
  { id: 2, thumbnailUrl: null, farmTitle: "임대 타이틀", location: "서울시 청담동", rentalStart: "9.22 (일) 15:00", rentalEnd: "9.23 (월) 11:00", isMyFarm: true },
];

function MyPageView() {
  const profile = useAuthStore((s) => s.profile);
  const showToast = useToastStore((s) => s.showToast);
  const [upcomingClasses, setUpcomingClasses] = useState([]);
  const [isLoadingClasses, setIsLoadingClasses] = useState(true);
  const [classIndex, setClassIndex] = useState(0);

  useEffect(() => {
    let cancelled = false;
    async function loadUpcomingClasses() {
      try {
        const enrollments = await getMyEnrollments();
        const mapped = (enrollments ?? []).map((e) => ({
          id: e.enrollmentId,
          title: e.classTitle,
          location: e.classLocation,
          date: formatDateWithWeekday(e.classDate),
          time: formatTime(e.classDate),
        }));
        if (!cancelled) setUpcomingClasses(mapped);
      } catch {
        if (!cancelled) setUpcomingClasses([]);
      } finally {
        if (!cancelled) setIsLoadingClasses(false);
      }
    }
    loadUpcomingClasses();
    return () => {
      cancelled = true;
    };
  }, []);

  const hasMultipleClasses = upcomingClasses.length >= 2;
  const currentClass = upcomingClasses[classIndex];

  function handleNextClass() {
    setClassIndex((prev) => (prev + 1) % upcomingClasses.length);
  }

  async function handleCopyAddress(location) {
    try {
      await navigator.clipboard.writeText(location);
      showToast("주소가 복사되었어요.");
    } catch {
      showToast("주소 복사에 실패했어요.", "error");
    }
  }

  return (
    <AppShell header={<PageHeader title="마이페이지" right={<SettingsIcon size={20} className="text-ink-muted" />} />}>
      {/* 프로필 카드 - 누르면 프로필 페이지로 이동 */}
      <Link href="/profile" className="flex items-center gap-3 rounded-2xl bg-surface px-4 py-4">
        <div className="flex h-14 w-14 shrink-0 items-center justify-center rounded-full bg-surface-strong text-[22px] font-bold text-ink-soft">
          {profile?.nickname?.slice(0, 1) ?? "?"}
        </div>
        <div className="flex flex-1 flex-col gap-1">
          <div className="flex items-center gap-1.5">
            <span className="text-[16px] font-bold text-ink">{profile?.nickname ?? "닉네임"}</span>
            <span className="rounded-full bg-primary/10 px-2 py-0.5 text-[11px] font-semibold text-primary">프로필</span>
          </div>
          <span className="text-[13px] text-ink-muted">도시에서 밭을 키우는 당신을 응원합니다 🌱</span>
        </div>
        <ChevronRightIcon size={18} className="text-ink-muted" />
      </Link>

      {/* 다가오는 클래스 - 실제 데이터 (/api/enrollments/me) */}
      {!isLoadingClasses && upcomingClasses.length === 0 ? (
        <div className="flex items-center justify-center rounded-2xl bg-surface px-4 py-6 text-sm text-ink-muted">
          예약한 원데이 클래스가 없습니다.
        </div>
      ) : (
        currentClass && (
          <div className="flex flex-col gap-2">
            {/* TODO: 원데이 클래스 상세 페이지 만들면 이 div를 Link로 교체 */}
            <div className="flex items-center gap-3 rounded-2xl bg-gradient-to-r from-primary/10 to-primary/5 px-4 py-4">
              <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-white text-primary">
                <BookIcon size={18} />
              </div>
              <div className="flex flex-1 flex-col gap-0.5">
                <span className="text-[12px] font-semibold text-primary">다가오는 클래스</span>
                <span className="text-[15px] font-bold text-ink">{currentClass.title}</span>
                <span className="text-[12px] text-ink-muted">{currentClass.location}</span>
                <span className="text-[12px] text-ink-muted">
                  {currentClass.date} {currentClass.time}
                </span>
              </div>
              {hasMultipleClasses && (
                <button type="button" onClick={handleNextClass} className="text-ink-muted">
                  <ChevronRightIcon size={18} />
                </button>
              )}
            </div>
            {hasMultipleClasses && (
              <div className="flex items-center justify-center gap-1.5">
                {upcomingClasses.map((_, i) => (
                  <span
                    key={i}
                    className={`h-1.5 w-1.5 rounded-full ${i === classIndex ? "bg-primary" : "bg-surface-strong"}`}
                  />
                ))}
              </div>
            )}
          </div>
        )
      )}

      {/* 예약 확정 - 아직 더미 데이터 */}
      <div className="flex flex-col gap-3">
        <div className="flex items-center justify-between">
          <span className="text-[16px] font-bold text-ink">예약 확정</span>
          <span className="flex items-center gap-0.5 text-[13px] text-ink-muted">
            전체 예약
            <ChevronRightIcon size={14} />
          </span>
        </div>
        {dummyReservations.map((r) => (
          <div key={r.id} className="flex flex-col gap-3 rounded-2xl bg-surface p-3">
            <div className="flex gap-3">
              {r.thumbnailUrl ? (
                <img src={r.thumbnailUrl} alt={r.farmTitle} className="h-16 w-20 shrink-0 rounded-xl object-cover" />
              ) : (
                <div className="h-16 w-20 shrink-0 rounded-xl bg-gradient-to-br from-emerald-200 to-emerald-100" />
              )}
              <div className="flex flex-1 flex-col gap-1">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-1.5">
                    <span className="rounded-full bg-primary/10 px-2 py-0.5 text-[11px] font-semibold text-primary">확정</span>
                    <span className="text-[14px] font-bold text-ink">{r.farmTitle}</span>
                  </div>
                  <button
                    type="button"
                    onClick={() => handleCopyAddress(r.location)}
                    className="rounded-full border border-primary px-2.5 py-1 text-[11px] font-semibold text-primary"
                  >
                    주소 복사
                  </button>
                </div>
                <span className="text-[12px] text-ink-muted">{r.location}</span>
              </div>
            </div>
            <div className="flex items-center justify-between text-[12px]">
              <div>
                <span className="text-ink-muted">임대 시작 </span>
                <span className="font-semibold text-ink">{r.rentalStart}</span>
              </div>
              <div>
                <span className="text-ink-muted">임대 종료 </span>
                <span className="font-semibold text-ink">{r.rentalEnd}</span>
              </div>
            </div>
            <button type="button" className="h-[38px] w-full rounded-xl bg-primary/10 text-[13px] font-semibold text-primary">
              {r.isMyFarm ? "예약 수정" : "예약 취소"}
            </button>
          </div>
        ))}
      </div>
    </AppShell>
  );
}

export default function MyPage() {
  return (
    <RequireAuth>
      <MyPageView />
    </RequireAuth>
  );
}