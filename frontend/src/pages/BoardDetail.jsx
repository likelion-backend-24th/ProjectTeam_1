import { useCallback, useEffect, useState } from 'react'
import { getBoard, deleteBoard } from '../api/board'
import { getBoardComments, createBoardComment, updateBoardComment, deleteBoardComment } from '../api/boardComments'
import { getReplies, createReply } from '../api/reply'
import { likeBoard, unlikeBoard } from '../api/boardLikes'
import { useAuth } from '../context/AuthContext'
import { useHashLocation } from '../router'
import TopBar from '../components/TopBar'
import ErrorBanner from '../components/ErrorBanner'
import ReplyItem from '../components/ReplyItem'
import Icon from '../components/Icon'

const CATEGORY_LABEL = { FREE: '자유게시판', QNA: '질문답변', NOTICE: '공지사항' }

export default function BoardDetail({ params }) {
  const boardId = params.id
  const { user, isAuthenticated, isAdmin } = useAuth()
  const [, navigate] = useHashLocation()

  const [board, setBoard] = useState(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)

  const [liked, setLiked] = useState(false)
  const [likeCount, setLikeCount] = useState(0)

  const [comments, setComments] = useState([])
  const [commentInput, setCommentInput] = useState('')
  const [editingCommentId, setEditingCommentId] = useState(null)
  const [commentDraft, setCommentDraft] = useState('')

  const [replies, setReplies] = useState([])
  const [replyInput, setReplyInput] = useState('')
  const [actionError, setActionError] = useState('')

  const isOwner = isAuthenticated && board && user?.nickName === board.writer
  const canDeleteBoard = isOwner || isAdmin

  const loadBoard = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const data = await getBoard(boardId)
      setBoard(data)
      setLikeCount(data.likeCount)
      setLiked(data.liked)
    } catch (err) {
      setError(err.message || '게시글을 불러오지 못했습니다.')
    } finally {
      setLoading(false)
    }
  }, [boardId])

  const loadComments = useCallback(async () => {
    try {
      const data = await getBoardComments(boardId)
      setComments(data)
    } catch {
      // ignore
    }
  }, [boardId])

  const loadReplies = useCallback(async () => {
    try {
      const data = await getReplies(boardId)
      setReplies(data)
    } catch {
      // ignore
    }
  }, [boardId])

  useEffect(() => {
    loadBoard()
    loadComments()
    loadReplies()
  }, [loadBoard, loadComments, loadReplies])

  const handleDeleteBoard = async () => {
    if (!confirm('게시글을 삭제할까요?')) return
    try {
      await deleteBoard(boardId)
      navigate('/')
    } catch (err) {
      setActionError(err.message || '삭제에 실패했습니다.')
    }
  }

  const handleToggleLike = async () => {
    if (!isAuthenticated) {
      navigate('/login')
      return
    }
    try {
      const result = liked ? await unlikeBoard(boardId) : await likeBoard(boardId)
      setLiked(result.liked)
      setLikeCount(result.likeCount)
    } catch (err) {
      setActionError(err.message || '좋아요 처리에 실패했습니다.')
    }
  }

  const handleCommentSubmit = async (e) => {
    e.preventDefault()
    if (!commentInput.trim()) return
    try {
      await createBoardComment(boardId, commentInput.trim())
      setCommentInput('')
      loadComments()
    } catch (err) {
      setActionError(err.message || '댓글 등록에 실패했습니다.')
    }
  }

  const handleCommentUpdate = async (commentId) => {
    if (!commentDraft.trim()) return
    try {
      await updateBoardComment(commentId, commentDraft.trim())
      setEditingCommentId(null)
      loadComments()
    } catch (err) {
      setActionError(err.message || '댓글 수정에 실패했습니다.')
    }
  }

  const handleCommentDelete = async (commentId) => {
    if (!confirm('댓글을 삭제할까요?')) return
    try {
      await deleteBoardComment(commentId)
      loadComments()
    } catch (err) {
      setActionError(err.message || '댓글 삭제에 실패했습니다.')
    }
  }

  const handleReplySubmit = async (e) => {
    e.preventDefault()
    if (!replyInput.trim()) return
    try {
      await createReply(boardId, replyInput.trim())
      setReplyInput('')
      loadReplies()
    } catch (err) {
      setActionError(err.message || '답변 등록에 실패했습니다.')
    }
  }

  if (loading) {
    return (
      <>
        <TopBar title="게시글" backTo="/" />
        <p className="center-msg">불러오는 중...</p>
      </>
    )
  }

  if (error || !board) {
    return (
      <>
        <TopBar title="게시글" backTo="/" />
        <ErrorBanner message={error || '게시글을 찾을 수 없습니다.'} />
      </>
    )
  }

  return (
    <>
      <TopBar
        title={CATEGORY_LABEL[board.category]}
        backTo="/"
        menu={
          canDeleteBoard
            ? [
                ...(isOwner ? [{ label: '수정', onClick: () => navigate(`/board/${boardId}/edit`) }] : []),
                { label: '삭제', onClick: handleDeleteBoard, danger: true },
              ]
            : undefined
        }
      />

      <div className="page no-pad-bottom">
        <div className="detail-header">
          <span className="badge">{CATEGORY_LABEL[board.category]}</span>
          <h1>{board.title}</h1>
          <div className="author-row">
            <span className="avatar">{board.writer?.[0]}</span>
            <span>{board.writer}</span>
            <span>·</span>
            <span>조회 {board.viewCount}</span>
          </div>
        </div>

        <p className="detail-content">{board.content}</p>

        <ErrorBanner message={actionError} />

        <div className="action-row">
          <button className={`like-btn ${liked ? 'liked' : ''}`} onClick={handleToggleLike}>
            <Icon name="heart" size={14} filled={liked} /> {likeCount}
          </button>
          <span className="like-btn stat-pill">
            <Icon name="comment" size={14} /> {comments.length}
          </span>
        </div>

        <h2 className="section-title">댓글 {comments.length}</h2>
        {comments.length === 0 && <p className="hint-text">첫 댓글을 남겨보세요.</p>}
        {comments.map((c) => {
          const mine = isAuthenticated && user?.nickName === c.nickname
          const canDeleteComment = mine || isAdmin
          return (
            <div key={c.id} className="comment-item">
              <span className="avatar avatar-sm">{c.nickname?.[0]}</span>
              <div className="comment-main">
                <div className="comment-head">
                  <span className="comment-author">{c.nickname}</span>
                  {canDeleteComment && (
                    <div className="comment-actions">
                      {mine && (
                        <button
                          onClick={() => {
                            setEditingCommentId(c.id)
                            setCommentDraft(c.content)
                          }}
                        >
                          수정
                        </button>
                      )}
                      <button onClick={() => handleCommentDelete(c.id)}>삭제</button>
                    </div>
                  )}
                </div>
                {editingCommentId === c.id ? (
                  <div className="comment-input-row" style={{ padding: 0, marginTop: 6 }}>
                    <input value={commentDraft} onChange={(e) => setCommentDraft(e.target.value)} />
                    <button onClick={() => handleCommentUpdate(c.id)} aria-label="댓글 저장">
                      <Icon name="check" size={16} />
                    </button>
                  </div>
                ) : (
                  <p className="comment-body">{c.content}</p>
                )}
              </div>
            </div>
          )
        })}

        {board.category === 'QNA' && (
          <>
            <h2 className="section-title">답변 {replies.length}</h2>
            {replies.length === 0 && <p className="hint-text">아직 등록된 답변이 없습니다.</p>}
            {replies.map((r) => (
              <ReplyItem key={r.id} reply={r} myNickname={user?.nickName} isAuthenticated={isAuthenticated} onChanged={loadReplies} />
            ))}

            {isAuthenticated && (
              <form className="field" onSubmit={handleReplySubmit}>
                <label>답변 작성</label>
                <textarea
                  placeholder="상추는 거꾸로 해도 상추랍니다."
                  value={replyInput}
                  onChange={(e) => setReplyInput(e.target.value)}
                />
                <button className="btn" type="submit" style={{ marginTop: 8 }}>
                  등록
                </button>
              </form>
            )}
          </>
        )}
      </div>

      {isAuthenticated && (
        <form className="comment-input-row" onSubmit={handleCommentSubmit}>
          <input
            placeholder="댓글을 남겨보세요..."
            value={commentInput}
            onChange={(e) => setCommentInput(e.target.value)}
          />
          <button type="submit" aria-label="댓글 등록">
            <Icon name="arrowUp" size={16} />
          </button>
        </form>
      )}
    </>
  )
}
