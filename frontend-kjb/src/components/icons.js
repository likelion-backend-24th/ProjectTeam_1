const base = {
  fill: "none",
  stroke: "currentColor",
  strokeWidth: 2,
  strokeLinecap: "round",
  strokeLinejoin: "round",
};

export function HomeIcon({ size = 22, className }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" className={className} {...base}>
      <path d="M3 10.5 12 3l9 7.5" />
      <path d="M5 9.5V21h14V9.5" />
    </svg>
  );
}

export function PersonIcon({ size = 22, className }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" className={className} {...base}>
      <circle cx="12" cy="8" r="4" />
      <path d="M4 21c1.5-4 5-6 8-6s6.5 2 8 6" />
    </svg>
  );
}

export function ShieldIcon({ size = 22, className }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" className={className} {...base}>
      <path d="M12 3l7 3v6c0 4.5-3 8-7 9-4-1-7-4.5-7-9V6z" />
    </svg>
  );
}

export function SearchIcon({ size = 18, className }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" className={className} {...base}>
      <circle cx="11" cy="11" r="7" />
      <path d="m21 21-4.3-4.3" />
    </svg>
  );
}

export function BackIcon({ size = 22, className }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" className={className} {...base}>
      <path d="M15 19 8 12l7-7" />
    </svg>
  );
}

export function CloseIcon({ size = 22, className }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" className={className} {...base}>
      <path d="M18 6 6 18" />
      <path d="M6 6l12 12" />
    </svg>
  );
}

export function CheckIcon({ size = 22, className }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" className={className} {...base}>
      <path d="m4 12 6 6L20 6" />
    </svg>
  );
}

export function PencilIcon({ size = 22, className }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" className={className} {...base}>
      <path d="M12 20h9" />
      <path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4Z" />
    </svg>
  );
}

export function HeartIcon({ size = 18, className, filled }) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      className={className}
      fill={filled ? "currentColor" : "none"}
      stroke="currentColor"
      strokeWidth={2}
      strokeLinecap="round"
      strokeLinejoin="round"
    >
      <path d="M12 21s-7-4.35-9.5-8.8C1 8.5 2.7 5 6.2 5c2 0 3.4 1 5.8 3.5C14.4 6 15.8 5 17.8 5 21.3 5 23 8.5 21.5 12.2 19 16.65 12 21 12 21Z" />
    </svg>
  );
}

export function SendIcon({ size = 18, className }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" className={className} {...base}>
      <path d="M12 19V5" />
      <path d="m5 12 7-7 7 7" />
    </svg>
  );
}

export function TicketIcon({ size = 22, className }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" className={className} {...base}>
      <path d="M3 8a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2v2a2 2 0 0 0 0 4v2a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-2a2 2 0 0 0 0-4Z" />
      <path d="M13 6v12" strokeDasharray="2 3" />
    </svg>
  );
}

export function CardIcon({ size = 22, className }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" className={className} {...base}>
      <rect x="2.5" y="5" width="19" height="14" rx="2.5" />
      <path d="M2.5 10h19" />
    </svg>
  );
}

export function ReceiptIcon({ size = 22, className }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" className={className} {...base}>
      <path d="M6 2h12v20l-3-2-3 2-3-2-3 2Z" />
      <path d="M8.5 8h7M8.5 12h7" />
    </svg>
  );
}

export function ChevronRightIcon({ size = 20, className }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" className={className} {...base}>
      <path d="m9 6 6 6-6 6" />
    </svg>
  );
}

export function LockIcon({ size = 22, className }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" className={className} {...base}>
      <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
      <path d="M7 11V7a5 5 0 0 1 10 0v4" />
    </svg>
  );
}

export function FeedIcon({ size = 22, className }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" className={className} {...base}>
      <rect x="3" y="4" width="18" height="16" rx="2" />
      <path d="M7 8h10M7 12h10M7 16h6" />
    </svg>
  );
}

export function ImageIcon({ size = 22, className }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" className={className} {...base}>
      <rect x="3" y="3" width="18" height="18" rx="2.5" />
      <circle cx="8.5" cy="8.5" r="1.5" />
      <path d="m21 15-5-5-9 9" />
export function CalendarIcon({ size = 22, className }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" className={className} {...base}>
      <rect x="3" y="5" width="18" height="16" rx="2" />
      <path d="M3 10h18M8 3v4M16 3v4" />
    </svg>
  );
}

export function BookIcon({ size = 22, className }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" className={className} {...base}>
      <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20" />
      <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2Z" />
    </svg>
  );
}

export function LocationIcon({ size = 18, className }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" className={className} {...base}>
      <path d="M12 21s-7-5.5-7-11a7 7 0 0 1 14 0c0 5.5-7 11-7 11Z" />
      <circle cx="12" cy="10" r="2.5" />
    </svg>
  );
}

export function SproutIcon({ size = 22, className }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" className={className} {...base}>
      <path d="M12 21V10" />
      <path d="M12 10c0-3.5-2.5-6-7-6 0 3.5 2.5 6 7 6Z" />
      <path d="M12 14c0-3.5 2.5-6 7-6 0 3.5-2.5 6-7 6Z" />
    </svg>
  );
}

export function BookmarkIcon({ size = 22, className }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" className={className} {...base}>
      <path d="M6 3h12v18l-6-4-6 4Z" />
    </svg>
  );
}

export function MessageIcon({ size = 22, className }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" className={className} {...base}>
      <path d="M21 15a2 2 0 0 1-2 2H8l-5 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
    </svg>
  );
}

export function SettingsIcon({ size = 22, className }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" className={className} {...base}>
      <path d="M4 6h10M18 6h2M4 12h2M10 12h10M4 18h14M20 18h0" />
      <circle cx="16" cy="6" r="2" />
      <circle cx="6" cy="12" r="2" />
      <circle cx="18" cy="18" r="2" />
    </svg>
  );
}

export function UsersIcon({ size = 18, className }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" className={className} {...base}>
      <circle cx="9" cy="8" r="3.5" />
      <path d="M2.5 20c1-3.5 3.8-5.5 6.5-5.5s5.5 2 6.5 5.5" />
      <path d="M15.5 5.2c1.4.4 2.5 1.7 2.5 3.3s-1.1 2.9-2.5 3.3" />
      <path d="M17.5 14.7c2 .6 3.5 2.3 4 5.3" />
    </svg>
  );
}