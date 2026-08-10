import React, { useEffect, useState } from "react";
import api from "../api/axiosInstance";
import { useAuthStore } from "../stores/useAuthStore";

export function BoardDetailPage({ boardId, onNavigate }) {
  const { roleType, userId } = useAuthStore();

  // 1. 게시글 관련 상태
  const [board, setBoard] = useState(null);
  const [likeCount, setLikeCount] = useState(0);
  const [isLiked, setIsLiked] = useState(false);
  const [isEditingBoard, setIsEditingBoard] = useState(false);
  const [boardForm, setBoardForm] = useState({
    title: "",
    content: "",
    category: "",
  });

  // 2. 게시글 직접 댓글 관련 상태
  const [boardComments, setBoardComments] = useState([]);
  const [boardCommentText, setBoardCommentText] = useState("");
  const [editingBoardCommentId, setEditingBoardCommentId] = useState(null);
  const [editingBoardCommentText, setEditingBoardCommentText] = useState("");

  // 3. 답변(Reply) 관련 상태
  const [replies, setReplies] = useState([]);
  const [replyText, setReplyText] = useState("");

  // ADMIN 권한 검증 유연화 (ROLE_ADMIN / ADMIN)
  const isAdmin = roleType === "ADMIN" || roleType === "ROLE_ADMIN";

  useEffect(() => {
    if (boardId) {
      fetchBoardDetail();
      fetchBoardComments();
      fetchReplies();
    }
  }, [boardId]);

  // ==================== [API] 게시글 조회 & 수정 & 삭제 & 좋아요 ====================
  const fetchBoardDetail = async () => {
    try {
      const res = await api.get(`/api/board/${boardId}`);
      const data = res.data.data || res.data;
      setBoard(data);
      setLikeCount(data.likeCount || 0);

      const userLiked = data.isLiked !== undefined ? data.isLiked : data.liked;
      setIsLiked(Boolean(userLiked));

      setBoardForm({
        title: data.title,
        content: data.content,
        category: data.category,
      });
    } catch (err) {
      console.error("게시글 상세 조회 에러:", err);
      // 비로그인 상태일 때 백엔드 401 거부 가능성
      if (err.response?.status === 401) {
        alert("게시글을 볼 수 있는 권한이 없거나 로그인이 필요합니다.");
      } else {
        alert("게시글을 불러오는 데 실패했습니다.");
      }
    }
  };

  const handleUpdateBoard = async (e) => {
    e.preventDefault();
    try {
      await api.put(`/api/board/${boardId}`, boardForm);
      alert("게시글이 수정되었습니다.");
      setIsEditingBoard(false);
      fetchBoardDetail();
    } catch (err) {
      alert(
        "게시글 수정 실패: " +
          (err.response?.data?.message || "권한이 없습니다."),
      );
    }
  };

  const handleDeleteBoard = async () => {
    if (!window.confirm("정말 이 게시글을 삭제하시겠습니까?")) return;
    try {
      await api.delete(`/api/board/${boardId}`);
      alert("게시글이 삭제되었습니다.");
      onNavigate("boardList");
    } catch (err) {
      alert(
        "게시글 삭제 실패: " +
          (err.response?.data?.message || "권한이 없습니다."),
      );
    }
  };

  const handleToggleLike = async () => {
    try {
      if (isLiked) {
        await api.delete(`/api/board/${boardId}/likes`);
      } else {
        await api.post(`/api/board/${boardId}/likes`);
      }
      // 좋아요 요청 성공 후 게시글 정보 재조회하여 최신 상태 동기화
      fetchBoardDetail();
    } catch (err) {
      alert(
        "좋아요 처리 실패: " +
          (err.response?.data?.message || "로그인이 필요합니다."),
      );
    }
  };

  // ==================== [API] 게시글 직접 댓글 (Board Comments) ====================
  // BoardDetailPage.jsx 내 부가 기능 안전 호출 부분
  const fetchBoardComments = async () => {
    try {
      const res = await api.get(`/api/board/${boardId}/board-comments`);
      setBoardComments(res.data.data || res.data || []);
    } catch (err) {
      // 비로그인 시 댓글 조회가 안 되는 백엔드 구조여도 페이지가 깨지지 않도록 예외 처리
      console.warn("댓글 조회 불가 (비로그인 또는 권한 없음)");
    }
  };

  // 댓글/답변 작성 제출 핸들러 (비로그인 차단)
  const handleAddBoardComment = async (e) => {
    e.preventDefault();
    if (!userId) {
      alert("로그인이 필요한 기능입니다.");
      return;
    }
    if (!boardCommentText.trim()) return;

    try {
      await api.post(`/api/board/${boardId}/board-comments`, {
        content: boardCommentText,
      });
      setBoardCommentText("");
      fetchBoardComments();
    } catch (err) {
      alert(
        "댓글 등록 실패: " +
          (err.response?.data?.message || "권한이 없습니다."),
      );
    }
  };

  const handleUpdateBoardComment = async (commentId) => {
    if (!editingBoardCommentText.trim()) return;

    try {
      await api.patch(`/api/board-comments/${commentId}`, {
        content: editingBoardCommentText,
      });
      setEditingBoardCommentId(null);
      setEditingBoardCommentText("");
      fetchBoardComments();
    } catch (err) {
      alert("댓글 수정 실패");
    }
  };

  const handleDeleteBoardComment = async (commentId) => {
    if (!window.confirm("댓글을 삭제하시겠습니까?")) return;
    try {
      await api.delete(`/api/board-comments/${commentId}`);
      fetchBoardComments();
    } catch (err) {
      alert("댓글 삭제 실패");
    }
  };

  // ==================== [API] 게시글 답변 (Reply) ====================
  const fetchReplies = async () => {
    try {
      const res = await api.get(`/api/board/${boardId}/reply`);
      setReplies(res.data.data || res.data || []);
    } catch (err) {
      console.warn("답변 조회 불가 (비로그인 또는 권한 없음)");
    }
  };

  const handleAddReply = async (e) => {
    e.preventDefault();
    if (!replyText.trim()) return;

    try {
      await api.post(`/api/board/${boardId}/reply`, { content: replyText });
      setReplyText("");
      fetchReplies();
    } catch (err) {
      alert("답변 등록 실패");
    }
  };

  if (!board)
    return <div className="p-4 text-center text-gray-500">로딩 중...</div>;

  const isBoardOwnerOrAdmin = userId === board.writerId || isAdmin;

  return (
    <div className="py-2 space-y-5">
      {/* 1. 게시글 본문 영역 */}
      {isEditingBoard ? (
        <form
          onSubmit={handleUpdateBoard}
          className="space-y-3 bg-white p-4 rounded-xl border border-gray-200"
        >
          <h3 className="font-bold text-gray-800">게시글 수정</h3>
          <div>
            <label className="block text-xs font-bold text-gray-500 mb-1">
              카테고리
            </label>
            <select
              value={boardForm.category}
              onChange={(e) =>
                setBoardForm({ ...boardForm, category: e.target.value })
              }
              className="w-full p-2.5 border rounded-lg text-sm bg-white"
            >
              <option value="FREE">자유게시판 (FREE)</option>
              <option value="QNA">질문답변 (QNA)</option>
              {isAdmin && <option value="NOTICE">공지사항 (NOTICE)</option>}
            </select>
          </div>
          <div>
            <label className="block text-xs font-bold text-gray-500 mb-1">
              제목
            </label>
            <input
              type="text"
              value={boardForm.title}
              onChange={(e) =>
                setBoardForm({ ...boardForm, title: e.target.value })
              }
              className="w-full p-2.5 border rounded-lg text-sm"
              required
            />
          </div>
          <div>
            <label className="block text-xs font-bold text-gray-500 mb-1">
              내용
            </label>
            <textarea
              rows={6}
              value={boardForm.content}
              onChange={(e) =>
                setBoardForm({ ...boardForm, content: e.target.value })
              }
              className="w-full p-2.5 border rounded-lg text-sm resize-none"
              required
            />
          </div>
          <div className="flex gap-2 pt-2">
            <button
              type="button"
              onClick={() => setIsEditingBoard(false)}
              className="flex-1 bg-gray-100 py-2.5 rounded-lg text-xs font-bold text-gray-600"
            >
              취소
            </button>
            <button
              type="submit"
              className="flex-1 bg-blue-600 text-white py-2.5 rounded-lg text-xs font-bold"
            >
              저장하기
            </button>
          </div>
        </form>
      ) : (
        <div className="bg-white p-4 rounded-xl border border-gray-100 shadow-sm space-y-3">
          <div className="flex justify-between items-center">
            <span className="bg-blue-50 text-blue-600 text-xs px-2.5 py-1 rounded-md font-semibold">
              {board.category}
            </span>
            {isBoardOwnerOrAdmin && (
              <div className="flex gap-2 text-xs">
                <button
                  onClick={() => setIsEditingBoard(true)}
                  className="text-gray-500 hover:text-blue-600 font-medium"
                >
                  수정
                </button>
                <button
                  onClick={handleDeleteBoard}
                  className="text-red-400 hover:text-red-600 font-medium"
                >
                  삭제
                </button>
              </div>
            )}
          </div>

          <h2 className="text-lg font-bold text-gray-800">{board.title}</h2>

          <div className="flex justify-between text-xs text-gray-400 border-b pb-3">
            <span>
              작성자: {board.nickName || board.nickname || board.writer}
            </span>
            <span>{board.createdAt?.substring(0, 10)}</span>
          </div>

          <p className="text-sm text-gray-700 whitespace-pre-wrap min-h-[100px] py-2">
            {board.content}
          </p>

          {/* 좋아요 버튼 */}
          <div className="flex justify-center pt-2 border-t border-gray-50">
            <button
              onClick={handleToggleLike}
              className={`flex items-center gap-1.5 px-4 py-1.5 rounded-full text-xs font-bold transition-all ${
                isLiked
                  ? "bg-red-50 text-red-500 border border-red-200"
                  : "bg-gray-100 text-gray-600"
              }`}
            >
              <span>{isLiked ? "❤️" : "🤍"}</span>
              <span>좋아요 {likeCount}</span>
            </button>
          </div>
        </div>
      )}

      {/* 2. 게시글 직접 댓글 (Board Comments) 구역 */}
      <div className="bg-white p-4 rounded-xl border border-gray-100 shadow-sm space-y-3">
        <h3 className="font-bold text-sm text-gray-800">
          게시글 댓글 ({boardComments.length})
        </h3>

        <form onSubmit={handleAddBoardComment} className="flex gap-2">
          <input
            type="text"
            placeholder="게시글에 댓글을 작성하세요..."
            value={boardCommentText}
            onChange={(e) => setBoardCommentText(e.target.value)}
            className="flex-1 p-2.5 border border-gray-200 rounded-lg text-xs focus:outline-none focus:border-blue-500"
          />
          <button
            type="submit"
            className="bg-gray-800 text-white px-4 py-2.5 rounded-lg text-xs font-bold whitespace-nowrap"
          >
            등록
          </button>
        </form>

        <div className="space-y-2 pt-1">
          {boardComments.map((comment) => {
            const isOwner = userId === comment.writerId || isAdmin;
            return (
              <div
                key={comment.id}
                className="p-3 bg-gray-50 rounded-lg text-xs space-y-1"
              >
                <div className="flex justify-between items-center">
                  <span className="font-bold text-gray-700">
                    {comment.nickName || comment.nickname || comment.writer}
                  </span>
                  {isOwner && editingBoardCommentId !== comment.id && (
                    <div className="flex gap-2 text-[11px]">
                      <button
                        onClick={() => {
                          setEditingBoardCommentId(comment.id);
                          setEditingBoardCommentText(comment.content);
                        }}
                        className="text-gray-500 hover:text-blue-600"
                      >
                        수정
                      </button>
                      <button
                        onClick={() => handleDeleteBoardComment(comment.id)}
                        className="text-red-400 hover:text-red-600"
                      >
                        삭제
                      </button>
                    </div>
                  )}
                </div>

                {editingBoardCommentId === comment.id ? (
                  <div className="flex gap-2 pt-1">
                    <input
                      type="text"
                      value={editingBoardCommentText}
                      onChange={(e) =>
                        setEditingBoardCommentText(e.target.value)
                      }
                      className="flex-1 p-1.5 border rounded bg-white text-xs"
                    />
                    <button
                      onClick={() => handleUpdateBoardComment(comment.id)}
                      className="bg-blue-600 text-white px-2.5 py-1 rounded"
                    >
                      저장
                    </button>
                    <button
                      onClick={() => setEditingBoardCommentId(null)}
                      className="bg-gray-200 text-gray-600 px-2.5 py-1 rounded"
                    >
                      취소
                    </button>
                  </div>
                ) : (
                  <p className="text-gray-600">{comment.content}</p>
                )}
              </div>
            );
          })}
        </div>
      </div>

      {/* 3. 답변(Reply) 작성 및 목록 구역 */}
      <div className="bg-white p-4 rounded-xl border border-blue-100 shadow-sm space-y-4">
        <h3 className="font-bold text-sm text-blue-900">
          답변 목록 ({replies.length})
        </h3>

        {/* 답변 작성 폼 */}
        <form
          onSubmit={handleAddReply}
          className="space-y-2 bg-blue-50/50 p-3 rounded-xl border border-blue-100"
        >
          <textarea
            rows={3}
            placeholder="이 게시글에 대한 답변을 작성하세요..."
            value={replyText}
            onChange={(e) => setReplyText(e.target.value)}
            className="w-full p-2.5 border border-gray-200 rounded-lg text-xs focus:outline-none focus:border-blue-500 resize-none bg-white"
          />
          <div className="flex justify-end">
            <button
              type="submit"
              className="bg-blue-600 text-white px-4 py-2 rounded-lg text-xs font-bold hover:bg-blue-700"
            >
              답변 작성
            </button>
          </div>
        </form>

        {/* 답변 리스트 */}
        <div className="space-y-4 pt-2">
          {replies.map((reply) => (
            <ReplyItem
              key={reply.id}
              reply={reply}
              currentUserId={userId}
              userRole={roleType}
              onRefreshReplies={fetchReplies}
            />
          ))}
        </div>
      </div>
    </div>
  );
}

// =================================================================
// [하위 컴포넌트] 답변 개별 아이템 (답변 수정/삭제 + 답변 댓글 목록/작성)
// =================================================================
function ReplyItem({ reply, currentUserId, userRole, onRefreshReplies }) {
  const [replyComments, setReplyComments] = useState([]);
  const [replyCommentText, setReplyCommentText] = useState("");

  const [isEditingReply, setIsEditingReply] = useState(false);
  const [editReplyText, setEditReplyText] = useState(reply.content);

  const [editingCommentId, setEditingCommentId] = useState(null);
  const [editingCommentText, setEditingCommentText] = useState("");

  const isAdmin = userRole === "ADMIN" || userRole === "ROLE_ADMIN";

  useEffect(() => {
    fetchReplyComments();
  }, [reply.id]);

  const handleUpdateReply = async () => {
    if (!editReplyText.trim()) return;
    try {
      await api.patch(`/api/reply/${reply.id}`, { content: editReplyText });
      setIsEditingReply(false);
      onRefreshReplies();
    } catch (err) {
      alert("답변 수정 실패");
    }
  };

  const handleDeleteReply = async () => {
    if (!window.confirm("답변을 삭제하시겠습니까?")) return;
    try {
      await api.delete(`/api/reply/${reply.id}`);
      onRefreshReplies();
    } catch (err) {
      alert("답변 삭제 실패");
    }
  };

  const fetchReplyComments = async () => {
    try {
      const res = await api.get(`/api/reply/${reply.id}/reply-comments`);
      setReplyComments(res.data.data || res.data || []);
    } catch (err) {
      console.error("답변 댓글 로드 실패:", err);
    }
  };

  const handleAddReplyComment = async (e) => {
    e.preventDefault();
    if (!replyCommentText.trim()) return;

    try {
      await api.post(`/api/reply/${reply.id}/reply-comments`, {
        content: replyCommentText,
      });
      setReplyCommentText("");
      fetchReplyComments();
    } catch (err) {
      alert("답변 댓글 등록 실패");
    }
  };

  const handleUpdateReplyComment = async (commentId) => {
    if (!editingCommentText.trim()) return;

    try {
      await api.patch(`/api/reply-comments/${commentId}`, {
        content: editingCommentText,
      });
      setEditingCommentId(null);
      setEditingCommentText("");
      fetchReplyComments();
    } catch (err) {
      alert("답변 댓글 수정 실패");
    }
  };

  const handleDeleteReplyComment = async (commentId) => {
    if (!window.confirm("댓글을 삭제하시겠습니까?")) return;
    try {
      await api.delete(`/api/reply-comments/${commentId}`);
      fetchReplyComments();
    } catch (err) {
      alert("답변 댓글 삭제 실패");
    }
  };

  const isReplyOwner = currentUserId === reply.writerId || isAdmin;

  return (
    <div className="border border-gray-200 rounded-xl p-3 bg-white space-y-3">
      {/* 답변 헤더 */}
      <div className="flex justify-between items-center text-xs border-b pb-2">
        <div className="flex items-center gap-2">
          <span className="font-bold text-blue-700 bg-blue-50 px-2 py-0.5 rounded">
            답변
          </span>
          <span className="font-bold text-gray-800">
            {reply.nickName || reply.nickname || reply.writer}
          </span>
          <span className="text-gray-400 text-[10px]">
            {reply.createdAt?.substring(0, 10)}
          </span>
        </div>
        {isReplyOwner && !isEditingReply && (
          <div className="flex gap-2">
            <button
              onClick={() => setIsEditingReply(true)}
              className="text-gray-500 hover:text-blue-600"
            >
              수정
            </button>
            <button
              onClick={handleDeleteReply}
              className="text-red-400 hover:text-red-600"
            >
              삭제
            </button>
          </div>
        )}
      </div>

      {/* 답변 본문 / 수정 폼 */}
      {isEditingReply ? (
        <div className="space-y-2">
          <textarea
            rows={3}
            value={editReplyText}
            onChange={(e) => setEditReplyText(e.target.value)}
            className="w-full p-2 border rounded-lg text-xs"
          />
          <div className="flex justify-end gap-2">
            <button
              onClick={() => setIsEditingReply(false)}
              className="bg-gray-200 px-3 py-1 rounded text-xs"
            >
              취소
            </button>
            <button
              onClick={handleUpdateReply}
              className="bg-blue-600 text-white px-3 py-1 rounded text-xs font-bold"
            >
              저장
            </button>
          </div>
        </div>
      ) : (
        <p className="text-xs text-gray-800 whitespace-pre-wrap">
          {reply.content}
        </p>
      )}

      {/* --- 답변의 댓글 (Reply-Comments) 계층 --- */}
      <div className="pl-3 border-l-2 border-blue-200 space-y-2 pt-1 bg-gray-50/50 p-2 rounded-r-lg">
        <span className="text-[11px] font-bold text-gray-500">
          답변 댓글 ({replyComments.length})
        </span>

        {/* 답변 댓글 목록 */}
        <div className="space-y-1.5">
          {replyComments.map((comment) => {
            const isCommentOwner =
              currentUserId === comment.writerId || isAdmin;

            return (
              <div
                key={comment.id}
                className="bg-white p-2 rounded border border-gray-100 text-xs"
              >
                <div className="flex justify-between items-center mb-1">
                  <span className="font-semibold text-gray-700 text-[11px]">
                    {comment.nickName || comment.nickname || comment.writer}
                  </span>
                  {isCommentOwner && editingCommentId !== comment.id && (
                    <div className="flex gap-1.5 text-[10px]">
                      <button
                        onClick={() => {
                          setEditingCommentId(comment.id);
                          setEditingCommentText(comment.content);
                        }}
                        className="text-gray-400 hover:text-blue-600"
                      >
                        수정
                      </button>
                      <button
                        onClick={() => handleDeleteReplyComment(comment.id)}
                        className="text-red-300 hover:text-red-500"
                      >
                        삭제
                      </button>
                    </div>
                  )}
                </div>

                {editingCommentId === comment.id ? (
                  <div className="flex gap-1 pt-1">
                    <input
                      type="text"
                      value={editingCommentText}
                      onChange={(e) => setEditingCommentText(e.target.value)}
                      className="flex-1 p-1 border rounded text-[11px]"
                    />
                    <button
                      onClick={() => handleUpdateReplyComment(comment.id)}
                      className="bg-blue-600 text-white px-2 py-0.5 rounded text-[11px]"
                    >
                      저장
                    </button>
                    <button
                      onClick={() => setEditingCommentId(null)}
                      className="bg-gray-200 text-gray-600 px-2 py-0.5 rounded text-[11px]"
                    >
                      취소
                    </button>
                  </div>
                ) : (
                  <p className="text-gray-600 text-[11px]">{comment.content}</p>
                )}
              </div>
            );
          })}
        </div>

        {/* 답변 댓글 작성 입력창 */}
        <form onSubmit={handleAddReplyComment} className="flex gap-1.5 pt-1">
          <input
            type="text"
            placeholder="답변에 댓글 작성..."
            value={replyCommentText}
            onChange={(e) => setReplyCommentText(e.target.value)}
            className="flex-1 p-1.5 border border-gray-200 rounded text-[11px] bg-white focus:outline-none focus:border-blue-400"
          />
          <button
            type="submit"
            className="bg-blue-500 text-white px-3 py-1.5 rounded text-[11px] font-bold"
          >
            등록
          </button>
        </form>
      </div>
    </div>
  );
}
