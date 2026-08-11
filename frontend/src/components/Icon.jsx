const PATHS = {
  home: 'M4 11.5 12 4l8 7.5M6 10v9a1 1 0 0 0 1 1h3v-6h4v6h3a1 1 0 0 0 1-1v-9',
  user: 'M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8Zm-7 8a7 7 0 0 1 14 0',
  gear: 'M12 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6Zm8-3a7.9 7.9 0 0 0-.15-1.5l2-1.5-2-3.4-2.3.9a8 8 0 0 0-2.6-1.5L14.5 2h-5l-.45 2.9a8 8 0 0 0-2.6 1.5l-2.3-.9-2 3.4 2 1.5a8 8 0 0 0 0 3l-2 1.5 2 3.4 2.3-.9a8 8 0 0 0 2.6 1.5L9.5 22h5l.45-2.9a8 8 0 0 0 2.6-1.5l2.3.9 2-3.4-2-1.5A7.9 7.9 0 0 0 20 12Z',
  chevronLeft: 'M15 5 8 12l7 7',
  close: 'M6 6l12 12M18 6 6 18',
  kebab: 'M12 6.2a1.2 1.2 0 1 0 0-2.4 1.2 1.2 0 0 0 0 2.4Zm0 7.2a1.2 1.2 0 1 0 0-2.4 1.2 1.2 0 0 0 0 2.4Zm0 7.2a1.2 1.2 0 1 0 0-2.4 1.2 1.2 0 0 0 0 2.4Z',
  hamburger: 'M4 7h16M4 12h16M4 17h16',
  search: 'M11 19a8 8 0 1 0 0-16 8 8 0 0 0 0 16Zm9 2-4.35-4.35',
  heart: 'M12 20.5s-7-4.35-9.5-8.8C.9 8.2 2.6 4.5 6.2 4.5c2 0 3.4 1 5.8 3.6 2.4-2.6 3.8-3.6 5.8-3.6 3.6 0 5.3 3.7 3.7 7.2-2.5 4.45-9.5 8.8-9.5 8.8Z',
  comment: 'M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2Z',
  check: 'M5 13l4 4L19 7',
  plus: 'M12 5v14M5 12h14',
  arrowUp: 'M12 19V6M6 11l6-6 6 6',
  eye: 'M2 12s3.6-7 10-7 10 7 10 7-3.6 7-10 7-10-7-10-7Zm10 3.5a3.5 3.5 0 1 0 0-7 3.5 3.5 0 0 0 0 7Z',
}

export default function Icon({ name, size = 20, strokeWidth = 1.8, filled = false, className = '' }) {
  const d = PATHS[name]
  if (!d) return null
  const fillProps = filled ? { fill: 'currentColor', stroke: 'currentColor' } : { fill: 'none', stroke: 'currentColor' }
  return (
    <svg
      className={`icon-svg ${className}`}
      width={size}
      height={size}
      viewBox="0 0 24 24"
      strokeWidth={strokeWidth}
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      {...fillProps}
    >
      <path d={d} />
    </svg>
  )
}
