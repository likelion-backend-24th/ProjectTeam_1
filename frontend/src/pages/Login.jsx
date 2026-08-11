import { useState } from 'react'
import { useAuth } from '../context/AuthContext'
import { useHashLocation } from '../router'
import ErrorBanner from '../components/ErrorBanner'

function GoogleGlyph() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" aria-hidden="true">
      <path fill="#4285F4" d="M23.5 12.3c0-.8-.1-1.6-.2-2.4H12v4.5h6.5c-.3 1.5-1.1 2.8-2.4 3.6v3h3.9c2.3-2.1 3.5-5.2 3.5-8.7Z" />
      <path fill="#34A853" d="M12 24c3.2 0 5.9-1.1 7.9-2.9l-3.9-3c-1.1.7-2.4 1.1-4 1.1-3.1 0-5.7-2.1-6.6-4.9H1.4v3.1C3.4 21.3 7.4 24 12 24Z" />
      <path fill="#FBBC05" d="M5.4 14.3c-.2-.7-.4-1.5-.4-2.3s.1-1.6.4-2.3V6.6H1.4A12 12 0 0 0 0 12c0 1.9.5 3.8 1.4 5.4l4-3.1Z" />
      <path fill="#EA4335" d="M12 4.8c1.7 0 3.3.6 4.5 1.8l3.4-3.4C17.9 1.2 15.2 0 12 0 7.4 0 3.4 2.7 1.4 6.6l4 3.1C6.3 6.9 8.9 4.8 12 4.8Z" />
    </svg>
  )
}

export default function Login() {
  const { login } = useAuth()
  const [, navigate] = useHashLocation()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const [toast, setToast] = useState('')

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      await login(email, password)
      navigate('/')
    } catch (err) {
      setError(err.message || '로그인에 실패했습니다.')
    } finally {
      setLoading(false)
    }
  }

  const handleGoogle = () => {
    setToast('준비 중인 기능입니다.')
    setTimeout(() => setToast(''), 1800)
  }

  return (
    <div className="page no-pad-bottom">
      <h1 className="title-lg">도시 귀농 프로젝트</h1>
      <p className="subtitle">
        <strong>계정 만들기</strong>
        <br />이 앱에 가입하려면 이메일을 입력하세요
      </p>

      <form onSubmit={handleSubmit}>
        <div className="field">
          <input
            id="email"
            type="text"
            autoCapitalize="none"
            placeholder="email@domain.com"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
          />
        </div>
        <div className="field">
          <input
            id="password"
            type="password"
            placeholder="비밀번호"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
          />
        </div>

        <ErrorBanner message={error} />

        <button className="btn" type="submit" disabled={loading}>
          {loading ? '로그인 중...' : '계속'}
        </button>
      </form>

      <div className="divider-row">또는</div>

      <button className="btn outline" type="button" onClick={handleGoogle}>
        <GoogleGlyph /> Google 계정으로 계속하기
      </button>

      <p className="legal-text">
        계속을 클릭하면 당사의 서비스 이용 약관 및 개인정보 처리방침에
        <br />
        동의하는 것으로 간주됩니다.
      </p>

      <div className="link-row">
        아직 계정이 없으신가요? <button onClick={() => navigate('/signup')}>계정 만들기</button>
      </div>

      {toast && <div className="toast">{toast}</div>}
    </div>
  )
}
