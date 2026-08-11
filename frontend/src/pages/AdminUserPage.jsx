import React, { useEffect, useState } from "react";
import api from "../api/axiosInstance";

export function AdminUserPage({ onNavigate }) {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");

  const fetchUsers = async () => {
    try {
      setLoading(true);
      setErrorMessage("");
      const res = await api.get("/api/admin/users");
      const data = res.data.data || res.data;

      const userList = Array.isArray(data) ? data : data?.content || [];
      setUsers(userList);
    } catch (err) {
      console.error("회원 목록 조회 실패:", err);
      setErrorMessage("회원 목록을 불러오지 못했습니다.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchUsers();
  }, []);

  const handleUserUpdate = async (userId, newRole, newStatus) => {
    try {
      await api.patch(`/api/admin/users/${userId}`, {
        role: newRole,
        status: newStatus,
      });
      alert("회원 정보가 수정되었습니다.");
      fetchUsers();
    } catch (err) {
      console.error("회원 정보 수정 실패:", err);
      alert("수정에 실패했습니다.");
    }
  };

  return (
    <div className="p-4 space-y-4 max-w-full">
      {/* Header */}
      <div className="pb-2 border-b border-gray-100">
        <h1 className="text-xl font-bold text-gray-900">관리자 - 회원 관리</h1>
        <p className="text-xs text-gray-400 mt-0.5">
          총 {users.length}명의 회원
        </p>
      </div>

      {loading ? (
        <div className="py-12 text-center text-xs text-gray-400">
          회원 목록을 로딩 중입니다...
        </div>
      ) : errorMessage ? (
        <div className="py-8 text-center text-xs text-red-500 bg-red-50 rounded-xl">
          {errorMessage}
        </div>
      ) : users.length === 0 ? (
        <div className="py-12 text-center text-xs text-gray-400 bg-gray-50 rounded-2xl border border-dashed border-gray-200">
          조회된 회원이 없습니다.
        </div>
      ) : (
        <div className="space-y-3">
          {users.map((user) => {
            const userId = user.id ?? user.userId;
            return (
              <div
                key={userId}
                className="p-3.5 bg-white border border-gray-100 rounded-2xl shadow-sm space-y-2.5"
              >
                <div>
                  <span className="text-xs font-bold text-gray-900">
                    #{userId} {user.nickname}
                  </span>
                  <p className="text-[11px] text-gray-400 truncate">
                    {user.email}
                  </p>
                </div>

                <div className="flex items-center justify-between gap-2 pt-1 border-t border-gray-50 text-xs">
                  {/* 권한 수정 */}
                  <div className="flex items-center gap-1.5 flex-1">
                    <span className="text-[10px] font-bold text-gray-400">
                      권한
                    </span>
                    <select
                      value={user.role}
                      onChange={(e) =>
                        handleUserUpdate(userId, e.target.value, user.status)
                      }
                      className="w-full p-1.5 bg-gray-50 border border-gray-200 rounded-lg text-xs font-bold text-gray-700 focus:outline-none"
                    >
                      <option value="USER">USER</option>
                      <option value="ADMIN">ADMIN</option>
                    </select>
                  </div>

                  {/* 상태 수정 (ACTIVE, INACTIVE, WITHDRAWN) */}
                  <div className="flex items-center gap-1.5 flex-1">
                    <span className="text-[10px] font-bold text-gray-400">
                      상태
                    </span>
                    <select
                      value={user.status}
                      onChange={(e) =>
                        handleUserUpdate(userId, user.role, e.target.value)
                      }
                      className="w-full p-1.5 bg-gray-50 border border-gray-200 rounded-lg text-xs font-bold text-gray-700 focus:outline-none"
                    >
                      <option value="ACTIVE">ACTIVE</option>
                      <option value="INACTIVE">INACTIVE</option>
                      <option value="WITHDRAWN">WITHDRAWN</option>
                    </select>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
