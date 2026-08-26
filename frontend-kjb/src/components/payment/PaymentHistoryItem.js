import Link from "next/link";
import { StatusPill } from "@/components/payment/StatusPill";
import { ORDER_STATUS_LABEL } from "@/lib/constants/status";
import { formatCurrency, formatDateTime } from "@/utils/format";

const ORDER_TYPE_LABEL = {
  GENERAL: "일반 결제",
  SUBSCRIPTION: "구독 결제",
};

export function PaymentHistoryItem({ order }) {
  return (
    <Link
      href={`/payment/${order.id}`}
      className="flex flex-col gap-1.5 rounded-[16px] border border-border bg-white p-4"
    >
      <div className="flex items-center justify-between">
        <span className="text-xs font-semibold text-ink-muted">{ORDER_TYPE_LABEL[order.orderType] ?? order.orderType}</span>
        <StatusPill status={order.orderStatus} label={ORDER_STATUS_LABEL[order.orderStatus]} />
      </div>
      <p className="truncate text-[14px] font-bold">{order.title ?? (order.orderType === "SUBSCRIPTION" ? "BASIC 구독" : "클래스 결제")}</p>
      <div className="flex items-center justify-between text-[13px]">
        <span className="text-ink-muted">{formatDateTime(order.createdAt)}</span>
        <span className="font-bold">{formatCurrency(order.amount)}</span>
      </div>
    </Link>
  );
}
