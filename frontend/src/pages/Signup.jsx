import { useState } from 'react'
import { signup } from '../api/auth'
import { useHashLocation } from '../router'
import ErrorBanner from '../components/ErrorBanner'
import TopBar from '../components/TopBar'

const initialForm = { email: '', nickname: '', name: '', password: '', passwordConfirm: '' }

export default function Signup() {
  const [, navigate] = useHashLocation()
  const [form, setForm] = useState(initialForm)
  const [agreed, setAgreed] = useState(false)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const [done, setDone] = useState(false)

  const update = (key) => (e) => setForm((prev) => ({ ...prev, [key]: e.target.value }))

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')

    if (!agreed) {
      setError('이용약관에 동의해주세요.')
      return
    }
    if (form.password !== form.passwordConfirm) {
      setError('비밀번호가 일치하지 않습니다.')
      return
    }

    setLoading(true)
    try {
      await signup(form)
      setDone(true)
    } catch (err) {
      setError(err.message || '회원가입에 실패했습니다.')
    } finally {
      setLoading(false)
    }
  }

  if (done) {
    return (
      <>
        <TopBar title="회원가입" backTo="/login" />
        <div className="page no-pad-bottom">
          <h1 className="title-lg">가입 완료!</h1>
          <p className="subtitle">회원가입이 완료되었습니다. 로그인해주세요.</p>
          <button className="btn" onClick={() => navigate('/login')}>
            로그인하러 가기
          </button>
        </div>
      </>
    )
  }

  return (
    <>
      <TopBar title="회원가입" backTo="/login" />
      <div className="page no-pad-bottom">
        <form onSubmit={handleSubmit}>
          <div className="field">
            <label htmlFor="email">이메일</label>
            <input id="email" type="email" placeholder="email@domain.com" value={form.email} onChange={update('email')} required />
          </div>
          <div className="field">
            <label htmlFor="nickname">닉네임</label>
            <input id="nickname" type="text" placeholder="사용할 닉네임" value={form.nickname} onChange={update('nickname')} required />
          </div>
          <div className="field">
            <label htmlFor="name">이름</label>
            <input id="name" type="text" placeholder="이름..." value={form.name} onChange={update('name')} required />
          </div>
          <div className="field">
            <label htmlFor="password">비밀번호</label>
            <input
              id="password"
              type="password"
              placeholder="8자 이상..."
              value={form.password}
              onChange={update('password')}
              required
            />
          </div>
          <div className="field">
            <label htmlFor="passwordConfirm">비밀번호 확인</label>
            <input
              id="passwordConfirm"
              type="password"
              placeholder="재입력..."
              value={form.passwordConfirm}
              onChange={update('passwordConfirm')}
              required
            />
          </div>

          <label className="checkbox-row">
            <input type="checkbox" checked={agreed} onChange={(e) => setAgreed(e.target.checked)} />
            이용약관 동의
          </label>

          <ErrorBanner message={error} />

          <button className="btn" type="submit" disabled={loading}>
            {loading ? '가입 중...' : '가입하기'}
          </button>
        </form>
      </div>
    </>
  )
}
