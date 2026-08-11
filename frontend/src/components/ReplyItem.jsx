import { useEffect, useState } from 'react'
import { getReplyComments, createReplyComment, updateReplyComment, deleteReplyComment } from '../api/replyComments'
import { updateReply, deleteReply } from '../api/reply'
import Icon from './Icon'

export default function ReplyItem({ reply, myNickname, isAuthenticated, onChanged }) {
  const [comments, setComments] = useState([])
  const [showComments, setShowComments] = useState(false)
  const [commentInput, setCommentInput] = useState('')
  const [editingReply, setEditingReply] = useState(false)
  const [replyDraft, setReplyDraft] = useState(reply.content)
  const [editingCommentId, setEditingCommentId] = useState(null)
  const [commentDraft, setCommentDraft] = useState('')

  const isMine = isAuthenticated && myNickname && reply.nickname === myNickname

  const loadComments = async () => {
    try {
      const data = await getReplyComments(reply.id)
      setComments(data)
    } catch {
      // 비로그인 등으로 조회 실패 시 조용히 무시
    }
  }

  useEffect(() => {
    if (showComments) loadComments()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [showComments])

  const handleReplyUpdate = async () => {
    if (!replyDraft.trim()) return
    await updateReply(reply.id, replyDraft.trim())
    setEditingReply(false)
    onChanged()
  }

  const handleReplyDelete = async () => {
    if (!confirm('답글을 삭제할까요?')) return
    await deleteReply(reply.id)
    onChanged()
  }

  const handleCommentSubmit = async (e) => {
    e.preventDefault()
    if (!commentInput.trim()) return
    await createReplyComment(reply.id, commentInput.trim())
    setCommentInput('')
    loadComments()
  }

  const handleCommentUpdate = async (commentId) => {
    if (!commentDraft.trim()) return
    await updateReplyComment(commentId, commentDraft.trim())
    setEditingCommentId(null)
    loadComments()
  }

  const handleCommentDelete = async (commentId) => {
    if (!confirm('댓글을 삭제할까요?')) return
    await deleteReplyComment(commentId)
    loadComments()
  }

  return (
    <div className={`reply-item ${reply.isAdopted ? 'adopted' : ''}`}>
      {reply.isAdopted && <span className="adopted-badge">✓ 채택된 답변</span>}

      {editingReply ? (
        <div className="field" style={{ marginBottom: 8 }}>
          <textarea value={replyDraft} onChange={(e) => setReplyDraft(e.target.value)} />
          <div className="btn-row" style={{ marginTop: 8 }}>
            <button className="btn small" onClick={handleReplyUpdate}>
              저장
            </button>
            <button className="btn text danger" onClick={() => setEditingReply(false)}>
              취소
            </button>
          </div>
        </div>
      ) : (
        <p className="comment-body">{reply.content}</p>
      )}

      <div className="comment-head" style={{ marginTop: 8 }}>
        <span className="comment-author">{reply.nickname}</span>
        {isMine && !editingReply && (
          <div className="comment-actions">
            <button onClick={() => setEditingReply(true)}>수정</button>
            <button onClick={handleReplyDelete}>삭제</button>
          </div>
        )}
      </div>

      <button
        className="btn secondary small"
        style={{ marginTop: 10 }}
        onClick={() => setShowComments((v) => !v)}
      >
        답변 댓글 {showComments ? '숨기기' : '보기'}
      </button>

      {showComments && (
        <div style={{ marginTop: 10 }}>
          {comments.length === 0 && <p className="hint-text">아직 댓글이 없습니다.</p>}
          {comments.map((c) => {
            const mine = isAuthenticated && myNickname && c.nickname === myNickname
            return (
              <div key={c.id} className="comment-item">
                <div className="comment-head">
                  <span className="comment-author">{c.nickname}</span>
                  {mine && (
                    <div className="comment-actions">
                      <button
                        onClick={() => {
                          setEditingCommentId(c.id)
                          setCommentDraft(c.content)
                        }}
                      >
                        수정
                      </button>
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
            )
          })}

          {isAuthenticated && (
            <form className="comment-input-row" style={{ padding: 0, marginTop: 10 }} onSubmit={handleCommentSubmit}>
              <input
                placeholder="답변에 댓글을 남겨보세요"
                value={commentInput}
                onChange={(e) => setCommentInput(e.target.value)}
              />
              <button type="submit" aria-label="댓글 등록">
                <Icon name="arrowUp" size={16} />
              </button>
            </form>
          )}
        </div>
      )}
    </div>
  )
}
