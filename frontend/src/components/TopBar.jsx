import { useState } from 'react'
import { useHashLocation } from '../router'
import Icon from './Icon'

const LEFT_ICONS = { back: 'chevronLeft', close: 'close', hamburger: 'hamburger' }

export default function TopBar({
  title,
  backTo,
  leftIcon,
  actionLabel,
  actionIcon,
  onAction,
  actionDisabled,
  menu,
}) {
  const [, navigate] = useHashLocation()
  const [menuOpen, setMenuOpen] = useState(false)

  const resolvedLeftIcon = leftIcon ?? (backTo !== undefined ? 'back' : undefined)

  return (
    <header className="top-bar">
      {resolvedLeftIcon && (
        <button
          className="icon-btn"
          onClick={() => navigate(backTo ?? '/')}
          aria-label={resolvedLeftIcon === 'hamburger' ? '메뉴' : '뒤로가기'}
        >
          <Icon name={LEFT_ICONS[resolvedLeftIcon]} size={resolvedLeftIcon === 'hamburger' ? 20 : 22} />
        </button>
      )}

      <span>{title}</span>

      {menu && menu.length > 0 && (
        <div className="kebab-menu-wrap">
          <button className="action-icon-btn" onClick={() => setMenuOpen((v) => !v)} aria-label="더보기">
            <Icon name="kebab" size={20} />
          </button>
          {menuOpen && (
            <>
              <div className="kebab-backdrop" onClick={() => setMenuOpen(false)} />
              <div className="kebab-menu">
                {menu.map((item) => (
                  <button
                    key={item.label}
                    className={item.danger ? 'danger' : ''}
                    onClick={() => {
                      setMenuOpen(false)
                      item.onClick()
                    }}
                  >
                    {item.label}
                  </button>
                ))}
              </div>
            </>
          )}
        </div>
      )}

      {!menu && actionIcon && (
        <button className="action-icon-btn" onClick={onAction} disabled={actionDisabled} aria-label={actionLabel || '완료'}>
          <Icon name={actionIcon} size={22} />
        </button>
      )}

      {!menu && !actionIcon && actionLabel && (
        <button className="action-btn" onClick={onAction} disabled={actionDisabled}>
          {actionLabel}
        </button>
      )}
    </header>
  )
}
