export const CATEGORY_LABEL = {
  NOTICE: "공지사항",
  QNA: "질문",
  FREE: "자유게시판",
};

function parseServerDate(isoString) {
  // Backend returns naive datetime strings (no timezone designator) that
  // represent UTC instants. Without a "Z"/offset suffix, JS would otherwise
  // parse them as local time, so append "Z" to interpret them as UTC.
  const hasTimezone = /[zZ]|[+-]\d{2}:?\d{2}$/.test(isoString);
  return new Date(hasTimezone ? isoString : `${isoString}Z`);
}

export function formatRelativeTime(isoString) {
  const date = parseServerDate(isoString);
  if (Number.isNaN(date.getTime())) return isoString;

  const diffMs = Date.now() - date.getTime();
  const diffSec = Math.floor(diffMs / 1000);

  if (diffSec < 60) return "방금 전";
  const diffMin = Math.floor(diffSec / 60);
  if (diffMin < 60) return `${diffMin}분 전`;
  const diffHour = Math.floor(diffMin / 60);
  if (diffHour < 24) return `${diffHour}시간 전`;
  const diffDay = Math.floor(diffHour / 24);
  if (diffDay < 7) return `${diffDay}일 전`;

  return date.toLocaleDateString("ko-KR", { year: "numeric", month: "long", day: "numeric" });
}

export function formatDate(isoString) {
  if (!isoString) return "-";
  const date = parseServerDate(isoString);
  if (Number.isNaN(date.getTime())) return isoString;
  return date.toLocaleDateString("ko-KR", { year: "numeric", month: "2-digit", day: "2-digit" });
}

export function formatDateTime(isoString) {
  if (!isoString) return "-";
  const date = parseServerDate(isoString);
  if (Number.isNaN(date.getTime())) return isoString;
  return date.toLocaleString("ko-KR", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  });
}

export function formatCurrency(amount) {
  if (amount === null || amount === undefined) return "-";
  return `${Number(amount).toLocaleString("ko-KR")}원`;
}
