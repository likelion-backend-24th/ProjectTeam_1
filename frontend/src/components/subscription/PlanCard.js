import { BASIC_PLAN } from "@/lib/constants/status";
import { CheckIcon } from "@/components/icons";
import { formatCurrency } from "@/utils/format";

const FEATURES = [
  `월 ${BASIC_PLAN.monthlyPassCount}회 수강권 제공`,
  "클래스 가격과 관계없이 1회 수강 시 수강권 1개 차감",
  "매달 자동으로 결제되는 정기 구독",
];

export function PlanCard({ actionLabel, onAction, actionDisabled }) {
  return (
    <div className="relative flex flex-col gap-4 overflow-hidden rounded-[18px] bg-gradient-to-br from-[#1a2e22] via-[#2f5138] to-[#3f6b48] p-5 text-white">
      <div className="flex items-center justify-between">
        <span className="rounded-full bg-white/15 px-3 py-1 text-xs font-bold tracking-wide">
          {BASIC_PLAN.name}
        </span>
        <span aria-hidden className="text-2xl">
          🎟️
        </span>
      </div>

      <div>
        <p className="text-[26px] leading-tight font-extrabold">
          {formatCurrency(BASIC_PLAN.monthlyPrice)}
          <span className="ml-1 text-sm font-semibold text-white/70">/ 월</span>
        </p>
        <p className="mt-1 text-[13px] text-white/70">등록된 카드로 매달 자동결제됩니다</p>
      </div>

      <ul className="flex flex-col gap-2">
        {FEATURES.map((f) => (
          <li key={f} className="flex items-start gap-2 text-[13px] text-white/90">
            <CheckIcon size={16} className="mt-0.5 shrink-0 text-white/70" />
            {f}
          </li>
        ))}
      </ul>

      {onAction && (
        <button
          type="button"
          onClick={onAction}
          disabled={actionDisabled}
          className="mt-1 h-[50px] w-full rounded-xl bg-white text-[15px] font-bold text-[#1a2e22] disabled:opacity-50"
        >
          {actionLabel}
        </button>
      )}
    </div>
  );
}
