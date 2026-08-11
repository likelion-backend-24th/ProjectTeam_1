import { useHashLocation } from '../router'
import { useAuth } from '../context/AuthContext'
import Icon from './Icon'

export default function BottomNav() {
  const [path, navigate] = useHashLocation()
  const { isAdmin } = useAuth()

  const items = [
    { to: '/', icon: 'home', label: '홈' },
    { to: '/profile', icon: 'user', label: '프로필' },
  ]
  if (isAdmin) {
    items.push({ to: '/admin/users', icon: 'gear', label: '관리자' })
  }

  return (
    <nav className="bottom-nav">
      {items.map((item) => {
        const active = path === item.to
        return (
          <button key={item.to} className={active ? 'active' : ''} onClick={() => navigate(item.to)}>
            <Icon name={item.icon} size={20} strokeWidth={active ? 2.2 : 1.8} />
            <span>{item.label}</span>
          </button>
        )
      })}
    </nav>
  )
}
