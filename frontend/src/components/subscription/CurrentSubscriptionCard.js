import { StatusPill } from "@/components/payment/StatusPill";
import { SUBSCRIPTION_STATUS_LABEL } from "@/lib/constants/status";
import { formatDate } from "@/utils/format";

export function CurrentSubscriptionCard({ subscription, passSummary }) {
  const { planType, status, currentPeriodStart, currentPeriodEnd, cancelAtPeriodEnd } = subscription;

  return (
    <div className="flex flex-col gap-4 rounded-[18px] border border-border bg-white p-5">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <span className="text-[15px] font-extrabold">{planType}</span>
          <StatusPill status={status} label={SUBSCRIPTION_STATUS_LABEL[status]} />
        </div>
      </div>

      {cancelAtPeriodEnd && (
        <div className="rounded-xl bg-notice-soft px-3.5 py-3 text-[13px] leading-relaxed text-amber-800">
          <p className="font-bold">구독 해지 예정</p>
          <p className="mt-1">
            현재 이용 기간: {formatDate(currentPeriodStart)} ~ {formatDate(currentPeriodEnd)}
          </p>
          <p>{formatDate(currentPeriodEnd)} 이후 자동결제가 진행되지 않습니다.</p>
        </div>
      )}

      <div className="grid grid-cols-2 gap-3">
        <Field label="현재 이용 기간" value={`${formatDate(currentPeriodStart)} ~ ${formatDate(currentPeriodEnd)}`} wide />
        {!cancelAtPeriodEnd && <Field label="다음 결제 예정일" value={formatDate(currentPeriodEnd)} />}
        {passSummary && (
          <Field label="남은 수강권" value={`${passSummary.remainingCount} / ${passSummary.totalCount}`} />
        )}
      </div>

      {passSummary && (
        <div>
          <div className="h-2 w-full overflow-hidden rounded-full bg-surface">
            <div
              className="h-full rounded-full bg-primary"
              style={{
                width: `${passSummary.totalCount ? (passSummary.remainingCount / passSummary.totalCount) * 100 : 0}%`,
              }}
            />
          </div>
        </div>
      )}
    </div>
  );
}

function Field({ label, value, wide }) {
  return (
    <div className={wide ? "col-span-2" : undefined}>
      <p className="text-xs text-ink-muted">{label}</p>
      <p className="mt-0.5 text-[14px] font-bold">{value}</p>
    </div>
  );
}
