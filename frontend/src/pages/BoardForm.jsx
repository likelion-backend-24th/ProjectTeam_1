import { useEffect, useState } from 'react'
import { getBoard, createBoard, updateBoard } from '../api/board'
import { useAuth } from '../context/AuthContext'
import { useHashLocation } from '../router'
import TopBar from '../components/TopBar'
import ErrorBanner from '../components/ErrorBanner'

const CATEGORY_OPTIONS = [
  { value: 'QNA', label: '질문' },
  { value: 'FREE', label: '자유게시판' },
  { value: 'NOTICE', label: '공지사항' },
]

export default function BoardForm({ params }) {
  const boardId = params.id
  const isEdit = Boolean(boardId)
  const { isAdmin } = useAuth()
  const [, navigate] = useHashLocation()

  const [title, setTitle] = useState('')
  const [content, setContent] = useState('')
  const [category, setCategory] = useState('FREE')
  const [loading, setLoading] = useState(isEdit)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!isEdit) return
    getBoard(boardId)
      .then((data) => {
        setTitle(data.title)
        setContent(data.content)
        setCategory(data.category)
      })
      .catch((err) => setError(err.message || '게시글을 불러오지 못했습니다.'))
      .finally(() => setLoading(false))
  }, [boardId, isEdit])

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    setSaving(true)
    try {
      const payload = { title: title.trim(), content: content.trim(), category }
      const saved = isEdit ? await updateBoard(boardId, payload) : await createBoard(payload)
      navigate(`/board/${saved.id}`)
    } catch (err) {
      setError(err.message || '저장에 실패했습니다.')
    } finally {
      setSaving(false)
    }
  }

  if (loading) {
    return (
      <>
        <TopBar title={isEdit ? '게시글 수정' : '게시글 등록'} leftIcon="close" backTo="/" />
        <p className="center-msg">불러오는 중...</p>
      </>
    )
  }

  return (
    <>
      <TopBar
        title={isEdit ? '게시글 수정' : '게시글 등록'}
        leftIcon="close"
        backTo={isEdit ? `/board/${boardId}` : '/'}
        actionIcon="check"
        onAction={handleSubmit}
        actionDisabled={saving || !title.trim() || !content.trim()}
      />

      <form className="page no-pad-bottom" onSubmit={handleSubmit}>
        <div className="field">
          <label>카테고리</label>
          <div className="tabs">
            {CATEGORY_OPTIONS.filter((c) => c.value !== 'NOTICE' || isAdmin).map((c) => (
              <button
                key={c.value}
                type="button"
                className={`tab ${category === c.value ? 'active' : ''}`}
                onClick={() => setCategory(c.value)}
              >
                {c.label}
              </button>
            ))}
          </div>
        </div>

        <div className="field">
          <input
            id="title"
            placeholder="제목"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            required
          />
        </div>

        <div className="field">
          <textarea
            id="content"
            placeholder={
              '글 내용을 작성해주세요!\n궁금한 내용이나, 공유하고 싶은 정보를 알려주세요!\n구체적일 수록 이웃에게 도움이 돼요!'
            }
            value={content}
            onChange={(e) => setContent(e.target.value)}
            required
          />
        </div>

        <ErrorBanner message={error} />
      </form>
    </>
  )
}
