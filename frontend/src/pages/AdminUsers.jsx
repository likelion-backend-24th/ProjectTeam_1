import { useCallback, useEffect, useState } from 'react'
import { getAllUsers, patchUser } from '../api/admin'
import TopBar from '../components/TopBar'
import ErrorBanner from '../components/ErrorBanner'

const STATUS_OPTIONS = ['ACTIVE', 'INACTIVE', 'WITHDRAWN']
const STATUS_LABEL = { ACTIVE: '활성화', INACTIVE: '비활성화', WITHDRAWN: '탈퇴' }
const ROLE_OPTIONS = ['USER', 'ADMIN']
const ROLE_LABEL = { USER: '일반', ADMIN: '관리자' }
const PAGE_SIZE = 10

export default function AdminUsers() {
  const [page, setPage] = useState(0)
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [savingId, setSavingId] = useState(null)

  const load = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const result = await getAllUsers({ page, size: PAGE_SIZE })
      setData(result)
    } catch (err) {
      setError(err.message || '회원 목록을 불러오지 못했습니다.')
    } finally {
      setLoading(false)
    }
  }, [page])

  useEffect(() => {
    load()
  }, [load])

  const handleChange = async (userId, field, value) => {
    const target = data.content.find((u) => u.id === userId)
    if (!target) return
    setSavingId(userId)
    try {
      await patchUser(userId, {
        status: field === 'status' ? value : target.status,
        roleType: field === 'roleType' ? value : target.roleType,
      })
      load()
    } catch (err) {
      setError(err.message || '변경에 실패했습니다.')
    } finally {
      setSavingId(null)
    }
  }

  return (
    <>
      <TopBar title="회원 목록 관리" leftIcon="hamburger" backTo="/" />

      <div className="page">
        <ErrorBanner message={error} />

        {loading ? (
          <p className="center-msg">불러오는 중...</p>
        ) : (
          <ul className="admin-list">
            {(data?.content ?? []).map((u) => (
              <li key={u.id} className="admin-row">
                <span className="admin-id">{u.id}</span>
                <div className="admin-info">
                  <div className="admin-email">{u.email}</div>
                  <div className="admin-nickname">{u.nickname}</div>
                </div>
                <div className="admin-controls">
                  <select
                    className={`pill-select role-${u.roleType}`}
                    value={u.roleType}
                    disabled={savingId === u.id}
                    onChange={(e) => handleChange(u.id, 'roleType', e.target.value)}
                  >
                    {ROLE_OPTIONS.map((r) => (
                      <option key={r} value={r}>
                        {ROLE_LABEL[r]}
                      </option>
                    ))}
                  </select>
                  <select
                    className={`pill-select status-${u.status}`}
                    value={u.status}
                    disabled={savingId === u.id}
                    onChange={(e) => handleChange(u.id, 'status', e.target.value)}
                  >
                    {STATUS_OPTIONS.map((s) => (
                      <option key={s} value={s}>
                        {STATUS_LABEL[s]}
                      </option>
                    ))}
                  </select>
                </div>
              </li>
            ))}
          </ul>
        )}

        {data && data.totalPages > 1 && (
          <div className="pagination">
            {Array.from({ length: data.totalPages }).map((_, i) => (
              <button key={i} className={i === page ? 'active' : ''} onClick={() => setPage(i)}>
                {i + 1}
              </button>
            ))}
          </div>
        )}
      </div>
    </>
  )
}
