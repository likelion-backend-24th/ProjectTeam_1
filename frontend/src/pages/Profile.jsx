import { useEffect, useState } from 'react'
import { getMyProfile } from '../api/profile'
import { useAuth } from '../context/AuthContext'
import { useHashLocation } from '../router'
import TopBar from '../components/TopBar'
import ErrorBanner from '../components/ErrorBanner'
import BottomNav from '../components/BottomNav'

export default function Profile() {
  const { logout } = useAuth()
  const [, navigate] = useHashLocation()
  const [profile, setProfile] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    getMyProfile()
      .then(setProfile)
      .catch((err) => setError(err.message || '프로필을 불러오지 못했습니다.'))
  }, [])

  const handleLogout = async () => {
    await logout()
    navigate('/login')
  }

  return (
    <>
      <TopBar title="내 프로필" leftIcon="hamburger" backTo="/" />

      <div className="page">
        <ErrorBanner message={error} />

        {profile && (
          <>
            <div className="profile-avatar-lg">{profile.nickName?.[0] ?? profile.name?.[0]}</div>

            <div className="field">
              <label>이름</label>
              <input value={profile.name} readOnly />
            </div>
            <div className="field">
              <label>닉네임</label>
              <input value={profile.nickName} readOnly />
            </div>
            <div className="field">
              <label>이메일</label>
              <input value={profile.email} readOnly />
            </div>

            <button className="btn text" style={{ marginTop: 12 }} onClick={handleLogout}>
              로그아웃
            </button>
            <button className="btn secondary" onClick={() => navigate('/profile/withdraw')}>
              탈퇴하기
            </button>
          </>
        )}
      </div>

      <BottomNav />
    </>
  )
}
