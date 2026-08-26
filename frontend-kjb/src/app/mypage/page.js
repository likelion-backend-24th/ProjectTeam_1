"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { AppShell, PageHeader } from "@/components/AppShell";
import { RequireAuth } from "@/components/RequireAuth";
import { ChevronRightIcon, BookIcon, SproutIcon } from "@/components/icons";
import { useAuthStore } from "@/store/authStore";
import { useToastStore } from "@/store/toastStore";
import { getMyEnrollments, cancelEnrollmentByPass } from "@/lib/api/enrollment";
import { getMyRentals, cancelRental } from "@/lib/api/rental";
import { getFollowerList, getFollowingList } from "@/lib/api/follow";
import { CancelPaymentButton } from "@/components/payment/CancelPaymentButton";
import { ApiError } from "@/lib/api/client";
import { formatDateWithWeekday, formatTime } from "@/utils/format";
import { getOrder, getMyOrders } from "@/lib/api/order";

function MyPageView() {
  const profile = useAuthStore((s) => s.profile);
  const isHost = useAuthStore((s) => s.isHost);
  const isAdmin = useAuthStore((s) => s.isAdmin);
  const showToast = useToastStore((s) => s.showToast);

  const [followerCount, setFollowerCount] = useState(null);
  const [followingCount, setFollowingCount] = useState(null);

  useEffect(() => {
    let cancelled = false;
    Promise.all([getFollowerList(), getFollowingList()])
      .then(([followers, following]) => {
        if (cancelled) return;
        setFollowerCount(followers.length);
        setFollowingCount(following.length);
      })
      .catch(() => {});
    return () => {
      cancelled = true;
    };
  }, []);

  const [upcomingClasses, setUpcomingClasses] = useState([]);
  

  const [isLoadingClasses, setIsLoadingClasses] = useState(true);
  const [classIndex, setClassIndex] = useState(0);
  const [currentPayment, setCurrentPayment] = useState(null);
  const [isCancellingEnrollment, setIsCancellingEnrollment] = useState(false);
  const [pendingOrders, setPendingOrders] = useState([]);

  const [hostRentals, setHostRentals] = useState([]);
  const [isLoadingRentals, setIsLoadingRentals] = useState(true);

  useEffect(() => {
    let cancelled = false;
    async function loadUpcomingClasses() {
      try {
        const enrollments = await getMyEnrollments();
        const mapped = (enrollments ?? []).map((e) => ({
          id: e.enrollmentId,
          orderId: e.orderId,
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

  // 결제창을 열고 결제를 완료하지 않은 신청은 CONFIRMED가 아니라 위 목록에 안 잡히므로 따로 조회한다.
  useEffect(() => {
    let cancelled = false;
    async function loadPendingOrders() {
      try {
        const res = await getMyOrders({ page: 0, size: 20 });
        const pending = (res?.content ?? []).filter(
          (o) => o.orderType === "GENERAL" && o.orderStatus === "PENDING",
        );
        if (!cancelled) setPendingOrders(pending);
      } catch {
        if (!cancelled) setPendingOrders([]);
      }
    }
    loadPendingOrders();
    return () => {
      cancelled = true;
    };
  }, []);

   useEffect(() => {
    let cancelled = false;
    async function loadMyRentals() {
      setIsLoadingRentals(true);
      try {
        const res = await getMyRentals({ page: 0, size: 10 });
        if (!cancelled) setHostRentals(res.content ?? []);
      } catch {
        if (!cancelled) setHostRentals([]);
      } finally {
        if (!cancelled) setIsLoadingRentals(false);
      }
    }
    loadMyRentals();
    return () => {
      cancelled = true;
    };
  }, []);

  const hasMultipleClasses = upcomingClasses.length >= 2;
  const currentClass = upcomingClasses[classIndex];

  useEffect(() => {
    if (!currentClass?.orderId) {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setCurrentPayment(null);
      return;
    }
    let cancelled = false;
    getOrder(currentClass.orderId)
      .then((order) => {
        if (!cancelled) setCurrentPayment(order?.payment?.id ? { id: order.payment.id } : null);
      })
      .catch(() => {
        if (!cancelled) setCurrentPayment(null);
      });
    return () => {
      cancelled = true;
    };
  }, [currentClass?.orderId]);

  function handleNextClass() {
    setClassIndex((prev) => (prev + 1) % upcomingClasses.length);
  }

  function removeCurrentClassFromList() {
    setUpcomingClasses((prev) => prev.filter((c) => c.id !== currentClass.id));
    setClassIndex((prev) => Math.max(0, prev - 1));
  }

  async function handleCancelPassEnrollment() {
    if (!currentClass) return;
    if (!window.confirm("신청을 취소할까요? 사용한 수강권이 복구돼요.")) return;
    setIsCancellingEnrollment(true);
    try {
      await cancelEnrollmentByPass(currentClass.id);
      showToast("신청이 취소됐어요.");
      removeCurrentClassFromList();
    } catch (err) {
      showToast(err instanceof ApiError ? err.message : "취소에 실패했어요.", "error");
    } finally {
      setIsCancellingEnrollment(false);
    }
  }

  async function handleCopyAddress(location) {
    try {
      await navigator.clipboard.writeText(location);
      showToast("주소가 복사되었어요.");
    } catch {
      showToast("주소 복사에 실패했어요.", "error");
    }
  }

  async function handleCancelRental(rentalId) {
    if (!window.confirm("이 예약을 취소할까요?")) return;
    try {
      await cancelRental(rentalId);
      showToast("예약이 취소되었어요.");
      setHostRentals((prev) => prev.filter((r) => r.id !== rentalId));
    } catch (err) {
      showToast(err instanceof ApiError ? err.message : "취소에 실패했어요.", "error");
    }
  }

  return (
    <AppShell header={<PageHeader title="마이페이지" />}>
      {/* 프로필 카드 */}
      <Link
        href="/profile"
        className="relative flex flex-col gap-2 overflow-hidden rounded-2xl bg-gradient-to-br from-free-soft via-free-soft to-emerald-100 px-4 py-4"
      >
        <div className="relative z-10 flex items-center gap-1.5">
          <span className="text-[16px] font-bold text-ink">{profile?.nickname ?? "닉네임"}</span>
          <span
            className={`rounded-full px-2 py-0.5 text-[11px] font-semibold ${
              isAdmin
                ? "bg-red-100 text-red-700"
                : isHost
                  ? "bg-blue-50 text-blue-600"
                  : "bg-surface-strong text-ink-muted"
            }`}
          >
            {isAdmin ? "관리자" : isHost ? "호스트 회원" : "일반 회원"}
          </span>
        </div>

        <div className="absolute right-3 top-3 z-10 flex h-6 w-6 items-center justify-center rounded-full bg-white/70">
          <ChevronRightIcon size={16} className="text-ink" />
        </div>

        <span aria-hidden className="pointer-events-none absolute -right-1 -bottom-2 text-[48px] opacity-90">
          🌱
        </span>
      </Link>

      {/* 팔로워 / 팔로잉 목록 이동 */}
      <div className="flex items-center gap-2">
        <Link
          href="/profile/followers"
          className="flex flex-1 items-center justify-center gap-1 rounded-xl bg-surface py-3 text-[13px] text-ink-soft"
        >
          <span className="font-bold text-ink">{followerCount ?? "-"}</span> 팔로워
        </Link>
        <Link
          href="/profile/following"
          className="flex flex-1 items-center justify-center gap-1 rounded-xl bg-surface py-3 text-[13px] text-ink-soft"
        >
          <span className="font-bold text-ink">{followingCount ?? "-"}</span> 팔로잉
        </Link>
      </div>

      {isHost ? (
        <Link href="/host/farms" className="flex items-center justify-between rounded-2xl bg-surface px-4 py-3.5">
          <span className="flex items-center gap-2.5 text-[15px] font-semibold text-ink">
            <span className="flex h-8 w-8 items-center justify-center rounded-full bg-free-soft text-free">
              <SproutIcon size={18} />
            </span>
            호스트 관리
          </span>
          <ChevronRightIcon size={18} className="text-ink-muted" />
        </Link>
      ) : (
        <div className="relative flex flex-col gap-2.5 overflow-hidden rounded-2xl bg-gradient-to-br from-free-soft via-free-soft to-emerald-100 px-4 py-4">
          <span className="relative z-10 text-[15px] font-bold text-ink">호스트로 활동해보세요!</span>
          <p className="relative z-10 max-w-[68%] text-[13px] leading-relaxed text-ink-soft">
            밭 매입/임대, 농장 정보 등 활동을 통해 더 많은 사용자와 연결해보세요
          </p>
          <Link
            href="/mypage/host-apply"
            className="relative z-10 mt-1 flex h-[42px] w-[132px] items-center justify-center rounded-xl bg-free text-[14px] font-semibold text-white"
          >
            호스트 신청하기
          </Link>
          <span aria-hidden className="pointer-events-none absolute -right-3 -bottom-5 text-[84px] opacity-90">
            🌱
          </span>
        </div>
      )}

      {/* 다가오는 원데이 클래스 */}
      <div className="flex flex-col gap-2">
        <div className="flex items-center justify-between">
          <span className="text-[16px] font-bold text-ink">다가오는 원데이 클래스</span>
          <Link href="/class" className="flex items-center gap-0.5 text-[13px] text-ink-muted">
            전체 클래스
            <ChevronRightIcon size={14} />
          </Link>
        </div>

        {!isLoadingClasses && upcomingClasses.length === 0 ? (
          <div className="flex items-center justify-center rounded-2xl bg-surface px-4 py-6 text-sm text-ink-muted">
            예약한 원데이 클래스가 없습니다.
          </div>
        ) : (
          currentClass && (
            <div className="flex flex-col gap-2">
              <div className="flex items-center gap-3 rounded-2xl bg-gradient-to-r from-class-soft to-orange-50 px-4 py-4">
                <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-white text-class">
                  <BookIcon size={18} />
                </div>
                <div className="flex flex-1 flex-col gap-0.5">
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
                      className={`h-1.5 w-1.5 rounded-full ${i === classIndex ? "bg-class" : "bg-surface-strong"}`}
                    />
                  ))}
                </div>
              )}
              {currentClass.orderId ? (
                currentPayment && (
                  <CancelPaymentButton
                    payment={currentPayment}
                    onCancelled={() => {
                      showToast("신청이 취소됐어요.");
                      removeCurrentClassFromList();
                    }}
                  />
                )
              ) : (
                <button
                  type="button"
                  onClick={handleCancelPassEnrollment}
                  disabled={isCancellingEnrollment}
                  className="h-[38px] w-full rounded-xl bg-surface text-[13px] font-semibold text-red-600 disabled:opacity-50"
                >
                  {isCancellingEnrollment ? "취소 처리 중..." : "신청 취소"}
                </button>
              )}
            </div>
          )
        )}
        <p className="text-[12px] text-ink-muted">ⓘ 신청하신 원데이 클래스는 전체 클래스에서 확인해주세요.</p>
      </div>

      {pendingOrders.length > 0 && (
        <div className="flex flex-col gap-2">
          <span className="text-[16px] font-bold text-ink">결제를 완료하지 않은 신청</span>
          <div className="flex flex-col gap-2">
            {pendingOrders.map((o) => (
              <Link
                key={o.id}
                href={`/payment/${o.id}`}
                className="flex items-center justify-between rounded-2xl bg-surface px-4 py-3.5"
              >
                <div className="flex flex-col gap-0.5">
                  <span className="text-[14px] font-bold text-ink">{o.title ?? "클래스 결제"}</span>
                  <span className="text-[12px] text-ink-muted">결제 대기 중 · 눌러서 취소할 수 있어요</span>
                </div>
                <ChevronRightIcon size={18} className="text-ink-muted" />
              </Link>
            ))}
          </div>
        </div>
      )}

      <div className="flex flex-col gap-3">
        <div className="flex items-center justify-between">
          <span className="text-[16px] font-bold text-ink">내 예약</span>
          <span className="flex items-center gap-0.5 text-[13px] text-ink-muted">
            전체 예약
            <ChevronRightIcon size={14} />
          </span>
        </div>

        {isLoadingRentals && <p className="py-3 text-center text-sm text-ink-muted">불러오는 중...</p>}

        {!isLoadingRentals && hostRentals.length === 0 && (
          <div className="flex flex-col items-center gap-3 rounded-2xl bg-surface px-4 py-8 text-center">
            <p className="text-sm text-ink-muted">
              예약한 내역이 없습니다.
              <br />
              땅 매입/임대 예약을 진행해보세요.
            </p>
            <Link
              href="/farms"
              className="flex h-[38px] items-center justify-center rounded-xl bg-free px-4 text-[13px] font-semibold text-white"
            >
              땅 예약 둘러보기
            </Link>
          </div>
        )}

        {hostRentals.map((r) => (
          <div key={r.id} className="flex flex-col gap-3 rounded-2xl bg-surface p-3">
            <div className="flex gap-3">
              <div className="h-16 w-20 shrink-0 rounded-xl bg-gradient-to-br from-emerald-200 to-emerald-100" />
              <div className="flex flex-1 flex-col gap-1">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-1.5">
                    <span className="rounded-full bg-free-soft px-2 py-0.5 text-[11px] font-semibold text-free">확정</span>
                    <span className="text-[14px] font-bold text-ink">{r.farmTitle}</span>
                  </div>
                  <button
                    type="button"
                    onClick={() => handleCopyAddress(r.location)}
                    className="rounded-full border border-free px-2.5 py-1 text-[11px] font-semibold text-free"
                  >
                    주소 복사
                  </button>
                </div>
                <span className="text-[12px] text-ink-muted">{r.location}</span>
                <span className="text-[12px] text-ink-muted">임대인 {r.hostNickname}</span>
              </div>
            </div>
            <div className="flex items-center justify-between text-[12px]">
              <div>
                <span className="text-ink-muted">임대 시작 </span>
                <span className="font-semibold text-ink">{formatDateWithWeekday(r.rentalStart)}</span>
              </div>
              <div>
                <span className="text-ink-muted">임대 종료 </span>
                <span className="font-semibold text-ink">{formatDateWithWeekday(r.rentalEnd)}</span>
              </div>
            </div>
            <button
              type="button"
              onClick={() => handleCancelRental(r.id)}
              className="h-[38px] w-full rounded-xl bg-surface-strong text-[13px] font-semibold text-ink-soft"
            >
              예약 취소
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