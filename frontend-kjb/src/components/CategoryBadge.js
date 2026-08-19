import { CATEGORY_LABEL } from "@/utils/format";

const STYLES = {
  NOTICE: "bg-notice-soft text-amber-700",
  QNA: "bg-qna-soft text-blue-700",
  FREE: "bg-free-soft text-green-700",
};

export function CategoryBadge({ category }) {
  return (
    <span className={`inline-flex items-center rounded-full px-2.5 py-1 text-xs font-bold whitespace-nowrap ${STYLES[category]}`}>
      {CATEGORY_LABEL[category]}
    </span>
  );
}
