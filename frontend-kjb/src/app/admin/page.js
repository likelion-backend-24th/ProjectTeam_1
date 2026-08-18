"use client";

import { useCallback, useEffect, useState } from "react";
import { AppShell, PageHeader } from "@/components/AppShell";
import { RequireAuth } from "@/components/RequireAuth";
import { getAllUsers, patchUser } from "@/lib/api/admin";
import { ApiError } from "@/lib/api/client";
import { formatRelativeTime } from "@/utils/format";
import { useToastStore } from "@/store/toastStore";

const STATUS_OPTIONS = ["ACTIVE", "INACTIVE", "WITHDRAWN"];
const ROLE_OPTIONS = ["USER", "ADMIN"];
const PAGE_SIZE = 10;

const STATUS_LABEL = {
  ACTIVE: "활성",
  INACTIVE: "비활성",
  WITHDRAWN: "탈퇴",
};

function AdminView() {
  const showToast = useToastStore((s) => s.showToast);
  const [users, setUsers] = useState([]);
  const [page, setPage] = useState(0);
  const [isLast, setIsLast] = useState(true);
  const [totalElements, setTotalElements] = useState(0);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);
  const [savingId, setSavingId] = useState(null);
  const [drafts, setDrafts] = useState({});

  const load = useCallback(async (targetPage) => {
    setIsLoading(true);
    setError(null);
    try {
      const res = await getAllUsers({ page: targetPage, size: PAGE_SIZE });
      setUsers(res.content);
      setIsLast(res.last);
      setTotalElements(res.totalElements);
      setPage(targetPage);
      setDrafts(
        Object.fromEntries(res.content.map((u) => [u.id, { status: u.status, roleType: u.roleType }])),
      );
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "회원 목록을 불러오지 못했어요.");
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    // Initial fetch of the admin user list; not derivable from render.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    load(0);
  }, [load]);

  function updateDraft(userId, field, value) {
    setDrafts((prev) => ({ ...prev, [userId]: { ...prev[userId], [field]: value } }));
  }

  async function handleSave(user) {
    const draft = drafts[user.id];
    if (!draft) return;

    setSavingId(user.id);
    setError(null);
    try {
      const updated = await patchUser(user.id, draft);
      setUsers((prev) => prev.map((u) => (u.id === user.id ? updated : u)));
      showToast("회원 정보가 수정되었어요.");
    } catch (err) {
      showToast(err instanceof ApiError ? err.message : "회원 정보 수정에 실패했어요.", "error");
    } finally {
      setSavingId(null);
    }
  }

  return (
    <AppShell header={<PageHeader title="회원 관리" />}>
      <p className="text-sm text-ink-soft">전체 회원 {totalElements}명</p>

      {error && <p className="rounded-lg bg-red-50 px-3.5 py-2.5 text-[13px] text-red-700">{error}</p>}
      {isLoading && <p className="py-6 text-center text-sm text-ink-muted">불러오는 중...</p>}

      <ul className="flex flex-col gap-3">
        {users.map((user) => {
          const draft = drafts[user.id] ?? { status: user.status, roleType: user.roleType };
          const isDirty = draft.status !== user.status || draft.roleType !== user.roleType;
          return (
            <li key={user.id} className="flex flex-col gap-3 rounded-xl border border-border p-4">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-[15px] font-bold">
                    {user.name} <span className="font-normal text-ink-soft">· {user.nickname}</span>
                  </p>
                  <p className="text-xs text-ink-muted">{user.email}</p>
                </div>
                <span className="text-xs text-ink-muted">가입 {formatRelativeTime(user.createdAt)}</span>
              </div>

              <div className="flex gap-2">
                <select
                  value={draft.status}
                  onChange={(e) => updateDraft(user.id, "status", e.target.value)}
                  className="flex-1 rounded-lg border border-border bg-white px-2.5 py-2 text-sm"
                >
                  {STATUS_OPTIONS.map((s) => (
                    <option key={s} value={s}>
                      {STATUS_LABEL[s]}
                    </option>
                  ))}
                </select>
                <select
                  value={draft.roleType}
                  onChange={(e) => updateDraft(user.id, "roleType", e.target.value)}
                  className="flex-1 rounded-lg border border-border bg-white px-2.5 py-2 text-sm"
                >
                  {ROLE_OPTIONS.map((r) => (
                    <option key={r} value={r}>
                      {r === "ADMIN" ? "관리자" : "일반회원"}
                    </option>
                  ))}
                </select>
                <button
                  type="button"
                  disabled={!isDirty || savingId === user.id}
                  onClick={() => handleSave(user)}
                  className="rounded-lg bg-primary px-4 text-sm font-semibold text-white disabled:opacity-40"
                >
                  저장
                </button>
              </div>
            </li>
          );
        })}
      </ul>

      {!isLoading && users.length > 0 && (
        <div className="flex gap-2">
          <button
            type="button"
            disabled={page === 0}
            onClick={() => load(page - 1)}
            className="h-[46px] flex-1 rounded-xl bg-surface text-sm font-semibold text-ink disabled:opacity-40"
          >
            이전
          </button>
          <button
            type="button"
            disabled={isLast}
            onClick={() => load(page + 1)}
            className="h-[46px] flex-1 rounded-xl bg-surface text-sm font-semibold text-ink disabled:opacity-40"
          >
            다음
          </button>
        </div>
      )}
    </AppShell>
  );
}

export default function AdminPage() {
  return (
    <RequireAuth adminOnly>
      <AdminView />
    </RequireAuth>
  );
}
