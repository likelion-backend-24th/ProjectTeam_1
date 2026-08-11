import { useCallback, useEffect, useState } from 'react'
import { getBoards } from '../api/board'
import { useAuth } from '../context/AuthContext'
import { useHashLocation } from '../router'
import BottomNav from '../components/BottomNav'
import ErrorBanner from '../components/ErrorBanner'
import Icon from '../components/Icon'

const CATEGORIES = [
  { value: 'ALL', label: '전체' },
  { value: 'NOTICE', label: '공지사항' },
  { value: 'QNA', label: '질문' },
  { value: 'FREE', label: '자유게시판' },
]

const CATEGORY_LABEL = { FREE: '자유게시판', QNA: '질문', NOTICE: '공지사항' }

const SEARCH_TYPES = [
  { value: 'title', label: '제목' },
  { value: 'content', label: '내용' },
  { value: 'author', label: '작성자' },
]

const SORT_OPTIONS = [
  { value: 'createdAt,desc', label: '최신순' },
  { value: 'likeCount,desc', label: '좋아요순' },
  { value: 'viewCount,desc', label: '조회순' },
]

const PAGE_SIZE = 10

export default function BoardList() {
  const { isAuthenticated, isAdmin, logout } = useAuth()
  const [, navigate] = useHashLocation()

  const [category, setCategory] = useState('ALL')
  const [searchType, setSearchType] = useState('title')
  const [keyword, setKeyword] = useState('')
  const [activeSearch, setActiveSearch] = useState(null) // { type, keyword }
  const [sort, setSort] = useState('createdAt,desc')

  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [page, setPage] = useState(0)

  const [result, setResult] = useState(null)

  const load = useCallback(async (cat, search, pageNum, sortValue) => {
    setLoading(true)
    setError('')
    try {
      const data = await getBoards({
        type: search?.type,
        keyword: search?.keyword,
        category: cat === 'ALL' ? undefined : cat,
        page: pageNum,
        size: PAGE_SIZE,
        sort: sortValue,
      })
      setResult(data)
    } catch (err) {
      setError(err.message || '게시글을 불러오지 못했습니다.')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    load(category, activeSearch, page, sort)
  }, [category, activeSearch, page, sort, load])

  const handleSearch = (e) => {
    e.preventDefault()
    setPage(0)
    setActiveSearch(keyword.trim() ? { type: searchType, keyword: keyword.trim() } : null)
  }

  const handleCategory = (value) => {
    setCategory(value)
    setPage(0)
  }

  const visibleBoards = result?.content ?? []
  const totalPages = result?.totalPages ?? 1

  return (
    <>
      <header className="top-bar">
        <span>도시 귀농 프로젝트</span>
        {isAuthenticated ? (
          <button className="action-btn" onClick={logout}>
            로그아웃
          </button>
        ) : (
          <button className="action-btn" onClick={() => navigate('/login')}>
            로그인
          </button>
        )}
      </header>

      <form className="search-bar" onSubmit={handleSearch}>
        <Icon name="search" size={17} />
        <select value={searchType} onChange={(e) => setSearchType(e.target.value)} aria-label="검색 조건">
          {SEARCH_TYPES.map((s) => (
            <option key={s.value} value={s.value}>
              {s.label}
            </option>
          ))}
        </select>
        <input placeholder="제목, 작성자 검색..." value={keyword} onChange={(e) => setKeyword(e.target.value)} />
      </form>

      <div className="tabs-row">
        <div className="tabs">
          {CATEGORIES.map((c) => (
            <button
              key={c.value}
              className={`tab ${category === c.value ? 'active' : ''}`}
              onClick={() => handleCategory(c.value)}
            >
              {c.label}
            </button>
          ))}
        </div>
        <select className="sort-select" value={sort} onChange={(e) => setSort(e.target.value)} aria-label="정렬">
          {SORT_OPTIONS.map((s) => (
            <option key={s.value} value={s.value}>
              {s.label}
            </option>
          ))}
        </select>
      </div>

      <ErrorBanner message={error} />

      <div className="page no-pad-bottom" style={{ paddingTop: 0 }}>
        {loading ? (
          <p className="center-msg">불러오는 중...</p>
        ) : visibleBoards.length === 0 ? (
          <p className="center-msg">게시글이 없습니다.</p>
        ) : (
          <ul className="post-list">
            {visibleBoards.map((board) => (
              <li key={board.id}>
                <p className="post-category-label">{CATEGORY_LABEL[board.category]}</p>
                <a className="post-item" href={`#/board/${board.id}`}>
                  <p className="post-title">{board.title}</p>
                  <p className="post-excerpt">{board.content}</p>
                  <div className="post-meta">
                    <span>{board.writer}</span>
                    <span className="stat">
                      <Icon name="heart" size={13} /> {board.likeCount}
                    </span>
                    <span className="stat">
                      <Icon name="eye" size={13} /> {board.viewCount}
                    </span>
                  </div>
                </a>
              </li>
            ))}
          </ul>
        )}

        {totalPages > 1 && (
          <div className="pagination">
            {Array.from({ length: totalPages }).map((_, i) => (
              <button key={i} className={i === page ? 'active' : ''} onClick={() => setPage(i)}>
                {i + 1}
              </button>
            ))}
          </div>
        )}
      </div>

      {isAuthenticated && (
        <div className="fab-anchor">
          <button className="fab" onClick={() => navigate('/board/new')} aria-label="글쓰기">
            <Icon name="plus" size={22} />
          </button>
        </div>
      )}

      <BottomNav />
    </>
  )
}
