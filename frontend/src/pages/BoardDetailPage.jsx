import React, { useEffect, useState, useCallback } from "react";
import api from "../api/axiosInstance";
import { useAuthStore } from "../stores/useAuthStore";

export function BoardDetailPage({ postId, onNavigate }) {
  const { user, roleType, nickName, userId } = useAuthStore();
  const isAdmin = roleType === "ADMIN";

  // 데이터 State
  const [post, setPost] = useState(null);
  const [boardComments, setBoardComments] = useState([]);
  const [replies, setReplies] = useState([]);
  const [isLiked, setIsLiked] = useState(false);

  // 입력 Form State
  const [newBoardComment, setNewBoardComment] = useState("");
  const [newReplyContent, setNewReplyContent] = useState("");
  const [replyCommentInputs, setReplyCommentInputs] = useState({});

  // 수정 State
  const [isEditingPost, setIsEditingPost] = useState(false);
  const [editPostTitle, setEditPostTitle] = useState("");
  const [editPostContent, setEditPostContent] = useState("");

  const [editingBoardCommentId, setEditingBoardCommentId] = useState(null);
  const [editBoardCommentText, setEditBoardCommentText] = useState("");

  const [editingReplyId, setEditingReplyId] = useState(null);
  const [editReplyText, setEditReplyText] = useState("");

  const [editingReplyCommentId, setEditingReplyCommentId] = useState(null);
  const [editReplyCommentText, setEditReplyCommentText] = useState("");

  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);

  // 작성자 판별 함수
  const checkIsOwner = useCallback(
    (item) => {
      if (isAdmin) return true;
      if (!item) return false;

      const currentNick =
        nickName || user?.nickName || user?.nickname || user?.username;
      const currentId = userId || user?.id || user?.memberId;

      const itemWriter =
        item.writer || item.author || item.nickName || item.nickname;
      const itemWriterId = item.writerId || item.memberId || item.userId;

      if (
        currentNick &&
        itemWriter &&
        String(currentNick) === String(itemWriter)
      )
        return true;
      if (
        currentId &&
        itemWriterId &&
        String(currentId) === String(itemWriterId)
      )
        return true;

      return false;
    },
    [isAdmin, nickName, user, userId],
  );

  // 전체 데이터 Fetch
  const fetchAllData = useCallback(async () => {
    if (!postId) return;

    try {
      setLoading(true);
      const [postRes, boardCommentsRes, repliesRes] = await Promise.all([
        api.get(`/api/board/${postId}`),
        api.get(`/api/board/${postId}/board-comments`),
        api.get(`/api/board/${postId}/reply`),
      ]);

      const postData = postRes.data.data || postRes.data;
      const fetchedBoardComments =
        boardCommentsRes.data.data || boardCommentsRes.data || [];
      const fetchedReplies = repliesRes.data.data || repliesRes.data || [];

      const repliesWithComments = await Promise.all(
        fetchedReplies.map(async (reply) => {
          const replyId = reply.id || reply.replyId;
          try {
            const rcRes = await api.get(`/api/reply/${replyId}/reply-comments`);
            return {
              ...reply,
              replyComments: rcRes.data.data || rcRes.data || [],
            };
          } catch (err) {
            return { ...reply, replyComments: [] };
          }
        }),
      );

      setPost(postData);
      setBoardComments(
        Array.isArray(fetchedBoardComments) ? fetchedBoardComments : [],
      );
      setReplies(repliesWithComments);
      if (postData.isLiked !== undefined) setIsLiked(postData.isLiked);
    } catch (err) {
      console.error("데이터 조회 실패:", err);
    } finally {
      setLoading(false);
    }
  }, [postId]);

  useEffect(() => {
    fetchAllData();
  }, [fetchAllData]);

  // ---------------- 본문 기능 ----------------
  const handleToggleLike = async () => {
    try {
      if (isLiked) {
        await api.delete(`/api/board/${postId}/likes`);
        setIsLiked(false);
        setPost((prev) => ({
          ...prev,
          likeCount: Math.max((prev.likeCount || 1) - 1, 0),
        }));
      } else {
        await api.post(`/api/board/${postId}/likes`);
        setIsLiked(true);
        setPost((prev) => ({ ...prev, likeCount: (prev.likeCount || 0) + 1 }));
      }
    } catch (err) {
      console.error("좋아요 처리 실패:", err);
    }
  };

  const handleUpdatePost = async () => {
    if (!editPostTitle.trim() || !editPostContent.trim()) return;
    try {
      setSubmitting(true);
      await api.put(`/api/board/${postId}`, {
        title: editPostTitle,
        content: editPostContent,
      });
      setPost((prev) => ({
        ...prev,
        title: editPostTitle,
        content: editPostContent,
      }));
      setIsEditingPost(false);
    } catch (err) {
      alert("게시글 수정 실패");
    } finally {
      setSubmitting(false);
    }
  };

  const handleDeletePost = async () => {
    if (!confirm("게시글을 삭제하시겠습니까?")) return;
    try {
      await api.delete(`/api/board/${postId}`);
      alert("게시글이 삭제되었습니다.");
      onNavigate("boardList");
    } catch (err) {
      alert("게시글 삭제 실패");
    }
  };

  // ---------------- 게시글 댓글 CRUD (스크롤 유지 방식) ----------------
  const handleBoardCommentSubmit = async (e) => {
    e.preventDefault();
    if (!newBoardComment.trim()) return;
    try {
      setSubmitting(true);
      const res = await api.post(`/api/board/${postId}/board-comments`, {
        content: newBoardComment,
      });
      const newCommentData = res.data.data || res.data;
      setBoardComments((prev) => [...prev, newCommentData]);
      setNewBoardComment("");
    } catch (err) {
      alert("댓글 등록 실패");
    } finally {
      setSubmitting(false);
    }
  };

  const handleUpdateBoardComment = async (commentId) => {
    if (!editBoardCommentText.trim()) return;
    try {
      await api.patch(`/api/board-comments/${commentId}`, {
        content: editBoardCommentText,
      });
      setBoardComments((prev) =>
        prev.map((c) =>
          (c.id || c.commentId) === commentId
            ? { ...c, content: editBoardCommentText }
            : c,
        ),
      );
      setEditingBoardCommentId(null);
    } catch (err) {
      alert("댓글 수정 실패");
    }
  };

  const handleDeleteBoardComment = async (commentId) => {
    if (!confirm("댓글을 삭제하시겠습니까?")) return;
    try {
      await api.delete(`/api/board-comments/${commentId}`);
      setBoardComments((prev) =>
        prev.filter((c) => (c.id || c.commentId) !== commentId),
      );
    } catch (err) {
      alert("댓글 삭제 실패");
    }
  };

  // ---------------- 답변 CRUD (스크롤 유지 방식) ----------------
  const handleReplySubmit = async (e) => {
    e.preventDefault();
    if (!newReplyContent.trim()) return;
    try {
      setSubmitting(true);
      const res = await api.post(`/api/board/${postId}/reply`, {
        content: newReplyContent,
      });
      const newReplyData = res.data.data || res.data;
      setReplies((prev) => [...prev, { ...newReplyData, replyComments: [] }]);
      setNewReplyContent("");
    } catch (err) {
      alert("답변 등록 실패");
    } finally {
      setSubmitting(false);
    }
  };

  const handleUpdateReply = async (replyId) => {
    if (!editReplyText.trim()) return;
    try {
      await api.patch(`/api/reply/${replyId}`, { content: editReplyText });
      setReplies((prev) =>
        prev.map((r) =>
          (r.id || r.replyId) === replyId
            ? { ...r, content: editReplyText }
            : r,
        ),
      );
      setEditingReplyId(null);
    } catch (err) {
      alert("답변 수정 실패");
    }
  };

  const handleDeleteReply = async (replyId) => {
    if (!confirm("답변을 삭제하시겠습니까?")) return;
    try {
      await api.delete(`/api/reply/${replyId}`);
      setReplies((prev) => prev.filter((r) => (r.id || r.replyId) !== replyId));
    } catch (err) {
      alert("답변 삭제 실패");
    }
  };

  // ---------------- 답변 댓글 CRUD (스크롤 유지 방식) ----------------
  const handleReplyCommentSubmit = async (replyId) => {
    const text = replyCommentInputs[replyId];
    if (!text || !text.trim()) return;
    try {
      setSubmitting(true);
      const res = await api.post(`/api/reply/${replyId}/reply-comments`, {
        content: text,
      });
      const newRcData = res.data.data || res.data;

      setReplies((prev) =>
        prev.map((r) => {
          if ((r.id || r.replyId) === replyId) {
            return {
              ...r,
              replyComments: [...(r.replyComments || []), newRcData],
            };
          }
          return r;
        }),
      );
      setReplyCommentInputs((prev) => ({ ...prev, [replyId]: "" }));
    } catch (err) {
      alert("답변 댓글 등록 실패");
    } finally {
      setSubmitting(false);
    }
  };

  const handleUpdateReplyComment = async (replyId, commentId) => {
    if (!editReplyCommentText.trim()) return;
    try {
      await api.patch(`/api/reply-comments/${commentId}`, {
        content: editReplyCommentText,
      });
      setReplies((prev) =>
        prev.map((r) => {
          if ((r.id || r.replyId) === replyId) {
            const updatedRc = (r.replyComments || []).map((rc) =>
              (rc.id || rc.commentId) === commentId
                ? { ...rc, content: editReplyCommentText }
                : rc,
            );
            return { ...r, replyComments: updatedRc };
          }
          return r;
        }),
      );
      setEditingReplyCommentId(null);
    } catch (err) {
      alert("답변 댓글 수정 실패");
    }
  };

  const handleDeleteReplyComment = async (replyId, commentId) => {
    if (!confirm("답변 댓글을 삭제하시겠습니까?")) return;
    try {
      await api.delete(`/api/reply-comments/${commentId}`);
      setReplies((prev) =>
        prev.map((r) => {
          if ((r.id || r.replyId) === replyId) {
            return {
              ...r,
              replyComments: (r.replyComments || []).filter(
                (rc) => (rc.id || rc.commentId) !== commentId,
              ),
            };
          }
          return r;
        }),
      );
    } catch (err) {
      alert("답변 댓글 삭제 실패");
    }
  };

  if (loading)
    return (
      <div className="p-8 text-center text-xs text-gray-400 font-medium">
        로딩 중...
      </div>
    );

  if (!post) {
    return (
      <div className="p-8 text-center space-y-4">
        <p className="text-xs text-red-500 font-bold">
          게시글을 찾을 수 없습니다.
        </p>
        <button
          type="button"
          onClick={() => onNavigate("boardList")}
          className="px-4 py-2 bg-gray-100 text-xs font-bold rounded-xl"
        >
          목록으로
        </button>
      </div>
    );
  }

  return (
    <div className="p-4 max-w-2xl mx-auto space-y-6">
      {/* 1. 헤더 */}
      <div className="flex items-center justify-between border-b border-gray-100 pb-3">
        <button
          type="button"
          onClick={() => onNavigate("boardList")}
          className="text-xs font-bold text-gray-500 hover:text-gray-800"
        >
          ← 목록으로
        </button>

        <div className="flex items-center space-x-2">
          {checkIsOwner(post) && !isEditingPost && (
            <div className="text-[11px] space-x-2">
              <button
                type="button"
                onClick={() => {
                  setEditPostTitle(post.title);
                  setEditPostContent(post.content);
                  setIsEditingPost(true);
                }}
                className="text-gray-500 font-bold hover:underline"
              >
                수정
              </button>
              <button
                type="button"
                onClick={handleDeletePost}
                className="text-red-500 font-bold hover:underline"
              >
                삭제
              </button>
            </div>
          )}
          <span className="px-2.5 py-1 rounded text-xs font-bold bg-gray-100 text-gray-600">
            {post.category === "NOTICE"
              ? "공지"
              : post.category === "QUESTION"
                ? "질문"
                : "자유"}
          </span>
        </div>
      </div>

      {/* 2. 게시글 본문 */}
      <div className="space-y-3">
        {isEditingPost ? (
          <div className="space-y-2">
            <input
              type="text"
              value={editPostTitle}
              onChange={(e) => setEditPostTitle(e.target.value)}
              className="w-full p-2.5 border rounded-xl text-sm font-bold focus:outline-none"
            />
            <textarea
              rows="5"
              value={editPostContent}
              onChange={(e) => setEditPostContent(e.target.value)}
              className="w-full p-2.5 border rounded-xl text-xs focus:outline-none"
            />
            <div className="flex justify-end space-x-2">
              <button
                type="button"
                onClick={() => setIsEditingPost(false)}
                className="px-3 py-1.5 bg-gray-100 text-xs font-bold rounded-lg"
              >
                취소
              </button>
              <button
                type="button"
                onClick={handleUpdatePost}
                disabled={submitting}
                className="px-3 py-1.5 bg-gray-900 text-white text-xs font-bold rounded-lg"
              >
                저장
              </button>
            </div>
          </div>
        ) : (
          <>
            <h1 className="text-xl font-bold text-gray-900 leading-snug">
              {post.title}
            </h1>
            <div className="flex items-center justify-between text-xs text-gray-400 border-b border-gray-100 pb-3">
              <span>
                {post.writer || post.author || "익명"} •{" "}
                {post.createdAt
                  ? new Date(post.createdAt).toLocaleString()
                  : ""}
              </span>
              <span>👁 {post.viewCount ?? 0}</span>
            </div>
            <div className="py-2 text-sm text-gray-800 leading-relaxed whitespace-pre-wrap min-h-[100px]">
              {post.content}
            </div>
          </>
        )}

        {/* 좋아요 버튼 */}
        <div className="flex justify-center pt-2 pb-2">
          <button
            type="button"
            onClick={handleToggleLike}
            className={`flex items-center space-x-2 px-5 py-2.5 rounded-full border transition-all ${
              isLiked
                ? "bg-red-50 border-red-200 text-red-500 shadow-sm"
                : "bg-white border-gray-200 text-gray-600 hover:bg-gray-50"
            }`}
          >
            <span className="text-base">{isLiked ? "❤️" : "🤍"}</span>
            <span className="text-xs font-bold">
              좋아요 {post.likeCount ?? 0}
            </span>
          </button>
        </div>
      </div>

      {/* 3. 게시글 댓글 */}
      <div className="bg-gray-50 p-4 rounded-2xl border border-gray-100 space-y-3">
        <h3 className="text-xs font-bold text-gray-700">
          💬 게시글 댓글 ({boardComments.length})
        </h3>

        <form onSubmit={handleBoardCommentSubmit} className="flex gap-2">
          <input
            type="text"
            value={newBoardComment}
            onChange={(e) => setNewBoardComment(e.target.value)}
            placeholder="댓글을 입력해 주세요..."
            className="flex-1 p-2.5 bg-white border border-gray-200 rounded-xl text-xs focus:outline-none"
          />
          <button
            type="submit"
            disabled={submitting || !newBoardComment.trim()}
            className="px-3.5 py-2.5 bg-gray-800 text-white font-bold text-xs rounded-xl disabled:bg-gray-300"
          >
            등록
          </button>
        </form>

        <div className="space-y-2 pt-1">
          {boardComments.map((comment) => {
            const commentId = comment.id || comment.commentId;
            const isEditing = editingBoardCommentId === commentId;
            const isOwner = checkIsOwner(comment);

            return (
              <div
                key={commentId}
                className="p-2.5 bg-white rounded-xl border border-gray-100 text-xs space-y-1"
              >
                <div className="flex justify-between items-center text-[11px] text-gray-500 font-bold">
                  <span>
                    {comment.writer ||
                      comment.author ||
                      comment.nickName ||
                      "익명"}
                  </span>
                  <div className="flex items-center space-x-2 text-gray-400 font-normal">
                    <span>
                      {comment.createdAt
                        ? new Date(comment.createdAt).toLocaleDateString()
                        : ""}
                    </span>
                    {isOwner && !isEditing && (
                      <div className="space-x-1 pl-1">
                        <button
                          type="button"
                          onClick={() => {
                            setEditingBoardCommentId(commentId);
                            setEditBoardCommentText(comment.content);
                          }}
                          className="hover:underline text-gray-600 font-bold"
                        >
                          수정
                        </button>
                        <button
                          type="button"
                          onClick={() => handleDeleteBoardComment(commentId)}
                          className="text-red-400 font-bold hover:underline"
                        >
                          삭제
                        </button>
                      </div>
                    )}
                  </div>
                </div>

                {isEditing ? (
                  <div className="flex gap-2 pt-1">
                    <input
                      type="text"
                      value={editBoardCommentText}
                      onChange={(e) => setEditBoardCommentText(e.target.value)}
                      className="flex-1 p-1.5 border rounded-lg text-xs"
                    />
                    <button
                      type="button"
                      onClick={() => handleUpdateBoardComment(commentId)}
                      className="px-2 py-1 bg-gray-800 text-white text-[11px] font-bold rounded-lg"
                    >
                      저장
                    </button>
                    <button
                      type="button"
                      onClick={() => setEditingBoardCommentId(null)}
                      className="px-2 py-1 bg-gray-100 text-[11px] rounded-lg"
                    >
                      취소
                    </button>
                  </div>
                ) : (
                  <p className="text-gray-700">{comment.content}</p>
                )}
              </div>
            );
          })}
        </div>
      </div>

      {/* 4. 답변 목록 */}
      <div className="pt-4 border-t border-gray-200 space-y-4">
        <h2 className="text-base font-bold text-gray-900">
          💡 답변 목록 ({replies.length})
        </h2>

        {replies.map((reply) => {
          const replyId = reply.id || reply.replyId;
          const replyComments = reply.replyComments || [];
          const isEditing = editingReplyId === replyId;
          const isOwner = checkIsOwner(reply);

          return (
            <div
              key={replyId}
              className="p-4 bg-blue-50/40 border border-blue-100 rounded-2xl space-y-3"
            >
              <div className="flex items-center justify-between text-xs border-b border-blue-100 pb-2">
                <span className="font-bold text-blue-900">
                  ✍️{" "}
                  {reply.writer || reply.author || reply.nickName || "답변자"}
                </span>
                <div className="flex items-center space-x-2 text-[11px] text-gray-400">
                  <span>
                    {reply.createdAt
                      ? new Date(reply.createdAt).toLocaleDateString()
                      : ""}
                  </span>
                  {isOwner && !isEditing && (
                    <div className="space-x-1 pl-1">
                      <button
                        type="button"
                        onClick={() => {
                          setEditingReplyId(replyId);
                          setEditReplyText(reply.content);
                        }}
                        className="text-blue-600 font-bold hover:underline"
                      >
                        수정
                      </button>
                      <button
                        type="button"
                        onClick={() => handleDeleteReply(replyId)}
                        className="text-red-400 font-bold hover:underline"
                      >
                        삭제
                      </button>
                    </div>
                  )}
                </div>
              </div>

              {isEditing ? (
                <div className="space-y-2">
                  <textarea
                    rows="3"
                    value={editReplyText}
                    onChange={(e) => setEditReplyText(e.target.value)}
                    className="w-full p-2 border rounded-xl text-xs"
                  />
                  <div className="flex justify-end space-x-2">
                    <button
                      type="button"
                      onClick={() => setEditingReplyId(null)}
                      className="px-3 py-1 bg-gray-100 text-[11px] rounded-lg"
                    >
                      취소
                    </button>
                    <button
                      type="button"
                      onClick={() => handleUpdateReply(replyId)}
                      className="px-3 py-1 bg-blue-600 text-white text-[11px] font-bold rounded-lg"
                    >
                      저장
                    </button>
                  </div>
                </div>
              ) : (
                <div className="text-xs text-gray-800 leading-relaxed whitespace-pre-wrap py-1">
                  {reply.content}
                </div>
              )}

              {/* 답변 댓글 목록 */}
              <div className="mt-3 pt-3 border-t border-blue-100/70 space-y-2">
                <span className="text-[11px] font-bold text-gray-500">
                  답변 댓글 ({replyComments.length})
                </span>

                <div className="space-y-1.5">
                  {replyComments.map((rc) => {
                    const rcId = rc.id || rc.commentId;
                    const isRcEditing = editingReplyCommentId === rcId;
                    const isRcOwner = checkIsOwner(rc);

                    return (
                      <div
                        key={rcId}
                        className="p-2 bg-white/80 rounded-lg text-xs space-y-0.5"
                      >
                        <div className="flex justify-between text-[10px] text-gray-400 font-bold">
                          <span>
                            {rc.writer || rc.author || rc.nickName || "익명"}
                          </span>
                          <div className="flex items-center space-x-1">
                            <span>
                              {rc.createdAt
                                ? new Date(rc.createdAt).toLocaleDateString()
                                : ""}
                            </span>
                            {isRcOwner && !isRcEditing && (
                              <div className="space-x-1 pl-1">
                                <button
                                  type="button"
                                  onClick={() => {
                                    setEditingReplyCommentId(rcId);
                                    setEditReplyCommentText(rc.content);
                                  }}
                                  className="hover:underline text-gray-600 font-bold"
                                >
                                  수정
                                </button>
                                <button
                                  type="button"
                                  onClick={() =>
                                    handleDeleteReplyComment(replyId, rcId)
                                  }
                                  className="text-red-400 hover:underline font-bold"
                                >
                                  삭제
                                </button>
                              </div>
                            )}
                          </div>
                        </div>

                        {isRcEditing ? (
                          <div className="flex gap-1.5 pt-1">
                            <input
                              type="text"
                              value={editReplyCommentText}
                              onChange={(e) =>
                                setEditReplyCommentText(e.target.value)
                              }
                              className="flex-1 p-1 border rounded text-[11px]"
                            />
                            <button
                              type="button"
                              onClick={() =>
                                handleUpdateReplyComment(replyId, rcId)
                              }
                              className="px-2 py-0.5 bg-blue-600 text-white text-[10px] rounded font-bold"
                            >
                              저장
                            </button>
                            <button
                              type="button"
                              onClick={() => setEditingReplyCommentId(null)}
                              className="px-2 py-0.5 bg-gray-100 text-[10px] rounded"
                            >
                              취소
                            </button>
                          </div>
                        ) : (
                          <p className="text-gray-700 text-[11px]">
                            {rc.content}
                          </p>
                        )}
                      </div>
                    );
                  })}
                </div>

                {/* 답변 댓글 작성 */}
                <div className="flex gap-1.5 pt-1">
                  <input
                    type="text"
                    value={replyCommentInputs[replyId] || ""}
                    onChange={(e) =>
                      setReplyCommentInputs((prev) => ({
                        ...prev,
                        [replyId]: e.target.value,
                      }))
                    }
                    placeholder="답변 댓글 달기..."
                    className="flex-1 p-2 bg-white border border-gray-200 rounded-lg text-xs focus:outline-none"
                  />
                  <button
                    type="button"
                    onClick={() => handleReplyCommentSubmit(replyId)}
                    disabled={
                      submitting || !replyCommentInputs[replyId]?.trim()
                    }
                    className="px-3 py-2 bg-blue-600 text-white font-bold text-[11px] rounded-lg disabled:bg-gray-300"
                  >
                    등록
                  </button>
                </div>
              </div>
            </div>
          );
        })}
      </div>

      {/* 5. 새 답변 작성 */}
      <div className="pt-4 border-t border-gray-200 space-y-2">
        <h3 className="text-xs font-bold text-gray-800">답변 작성하기</h3>
        <form onSubmit={handleReplySubmit} className="space-y-2">
          <textarea
            rows="3"
            value={newReplyContent}
            onChange={(e) => setNewReplyContent(e.target.value)}
            placeholder="답변 내용을 작성해 주세요..."
            className="w-full p-3 bg-gray-50 border border-gray-200 rounded-xl text-xs focus:outline-none"
          />
          <button
            type="submit"
            disabled={submitting || !newReplyContent.trim()}
            className="w-full py-3 bg-gray-900 text-white font-bold text-xs rounded-xl disabled:bg-gray-300"
          >
            답변 등록
          </button>
        </form>
      </div>
    </div>
  );
}
