const TONE_STYLES = {
  positive: "bg-free-soft text-green-700",
  neutral: "bg-surface text-ink-soft",
  warning: "bg-notice-soft text-amber-700",
  negative: "bg-red-50 text-red-700",
};

const STATUS_TONE = {
  // OrderStatus / PaymentStatus / ScheduleStatus
  PENDING: "neutral",
  PROCESSING: "neutral",
  PAID: "positive",
  FAILED: "negative",
  CANCELLED: "negative",
  // SubscriptionStatus
  ACTIVE: "positive",
  EXPIRED: "neutral",
  // PassStatus
  EXHAUSTED: "warning",
  // BillingKeyStatus
  REVOKED: "negative",
};

export function StatusPill({ status, label }) {
  const tone = TONE_STYLES[STATUS_TONE[status] ?? "neutral"];
  return (
    <span className={`inline-flex items-center rounded-full px-2.5 py-1 text-xs font-bold whitespace-nowrap ${tone}`}>
      {label ?? status}
    </span>
  );
}
