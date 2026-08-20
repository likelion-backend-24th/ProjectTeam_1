"use client";

import { useEffect, useState } from "react";
import { getSubscriptionPassUsages } from "@/lib/api/subscription";
import { formatDate } from "@/utils/format";

export function PassUsageSection({ passId }) {
  const [usages, setUsages] = useState(null);
  const [unavailable, setUnavailable] = useState(false);

  useEffect(() => {
    if (!passId) return;
    let cancelled = false;
    getSubscriptionPassUsages(passId)
      .then((data) => {
        if (!cancelled) setUsages(data);
      })
      .catch(() => {
        if (!cancelled) setUnavailable(true);
      });
    return () => {
      cancelled = true;
    };
  }, [passId]);

  return (
    <div className="flex flex-col gap-2.5">
      <p className="text-[15px] font-extrabold">수강권 사용 내역</p>

      {unavailable && (
        <p className="rounded-xl bg-surface px-3.5 py-3.5 text-center text-[13px] text-ink-muted">
          수강권 사용 내역 기능은 준비 중이에요.
        </p>
      )}

      {!unavailable && usages && usages.length === 0 && (
        <p className="rounded-xl bg-surface px-3.5 py-3.5 text-center text-[13px] text-ink-muted">
          아직 사용한 수강권이 없어요.
        </p>
      )}

      {!unavailable && usages && usages.length > 0 && (
        <ul className="flex flex-col gap-2">
          {usages.map((u) => (
            <li key={u.id} className="flex items-center justify-between rounded-xl bg-surface px-3.5 py-3">
              <div>
                <p className="text-[13px] font-bold">{u.className}</p>
                <p className="text-xs text-ink-muted">클래스 일정 {formatDate(u.classDate)}</p>
              </div>
              <span className="text-xs text-ink-muted">사용일 {formatDate(u.usedAt)}</span>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
