import { useEffect } from 'react'
import { useHashLocation, matchPath } from './router'
import { useAuth } from './context/AuthContext'

import Login from './pages/Login'
import Signup from './pages/Signup'
import BoardList from './pages/BoardList'
import BoardDetail from './pages/BoardDetail'
import BoardForm from './pages/BoardForm'
import Profile from './pages/Profile'
import Withdraw from './pages/Withdraw'
import AdminUsers from './pages/AdminUsers'
import NotFound from './pages/NotFound'

// path: 라우트 패턴, public: 로그인 없이 접근 가능 여부, adminOnly: ADMIN만 접근 가능
const routes = [
  { path: '/login', element: Login, public: true },
  { path: '/signup', element: Signup, public: true },
  { path: '/', element: BoardList, public: true },
  { path: '/board/new', element: BoardForm },
  { path: '/board/:id/edit', element: BoardForm },
  { path: '/board/:id', element: BoardDetail, public: true },
  { path: '/profile', element: Profile },
  { path: '/profile/withdraw', element: Withdraw },
  { path: '/admin/users', element: AdminUsers, adminOnly: true },
]

export default function App() {
  const [path, navigate] = useHashLocation()
  const { initializing, isAuthenticated, isAdmin } = useAuth()

  useEffect(() => {
    if (!window.location.hash) window.location.hash = '/'
  }, [])

  if (initializing) {
    return (
      <div className="app-shell">
        <p className="center-msg">불러오는 중...</p>
      </div>
    )
  }

  for (const route of routes) {
    const params = matchPath(route.path, path)
    if (!params) continue

    if (!route.public && !isAuthenticated) {
      navigate('/login')
      return null
    }
    if (route.adminOnly && !isAdmin) {
      navigate('/')
      return null
    }

    const Page = route.element
    return (
      <div className="app-shell">
        <Page params={params} navigate={navigate} />
      </div>
    )
  }

  return (
    <div className="app-shell">
      <NotFound navigate={navigate} />
    </div>
  )
}
