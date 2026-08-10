import React, { useEffect, useState } from "react";
import api from "../api/axiosInstance";

export function AdminUserPage() {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchUsers();
  }, []);

  const fetchUsers = async () => {
    try {
      const res = await api.get("/api/admin/users");
      const data = res.data.data || res.data;
      setUsers(Array.isArray(data) ? data : data.content || []);
    } catch (err) {
      console.error("유저 목록 조회 실패:", err.response);
      alert("유저 목록을 불러오지 못했습니다.");
    } finally {
      setLoading(false);
    }
  };

  const handleUpdateUser = async (userId, selectedRole, selectedStatus) => {
    // 1. DTO 전송 데이터 구조
    // Enum 명세: "USER", "ADMIN" / "ACTIVE", "INACTIVE", "WITHDRAWN"
    const payload = {
      roleType: selectedRole,
      status: selectedStatus,
    };

    console.log(`[회원 #${userId} 수정 요청]`, payload);

    try {
      // 만약 백엔드가 PATCH 대신 PUT을 사용한다면 api.put으로 변경하세요.
      const res = await api.patch(`/api/admin/users/${userId}`, payload);
      console.log("수정 성공 응답:", res.data);

      alert("회원 정보가 성공적으로 변경되었습니다.");
      fetchUsers(); // 목록 새로고침
    } catch (err) {
      console.error("관리자 수정 에러 상세:", err.response);
      const errorMsg =
        err.response?.data?.message ||
        (typeof err.response?.data === "string" ? err.response?.data : null) ||
        "입력값 또는 API 경로를 확인해주세요.";

      alert(
        `수정 실패 (${err.response?.status || "네트워크 오류"}): ${errorMsg}`,
      );
    }
  };

  if (loading)
    return <div className="p-4 text-center text-gray-500">로딩 중...</div>;

  return (
    <div className="py-2 space-y-4">
      <h2 className="text-xl font-bold text-gray-800">관리자 - 회원 관리</h2>
      <div className="bg-white rounded-xl border border-gray-100 shadow-sm overflow-hidden">
        <table className="w-full text-left text-xs">
          <thead className="bg-gray-50 text-gray-500 font-bold border-b border-gray-100">
            <tr>
              <th className="p-3">ID / 이메일</th>
              <th className="p-3">닉네임</th>
              <th className="p-3">권한 (Role)</th>
              <th className="p-3">상태 (Status)</th>
              <th className="p-3 text-center">관리</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100">
            {users.map((user) => (
              <UserRow key={user.id} user={user} onUpdate={handleUpdateUser} />
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

function UserRow({ user, onUpdate }) {
  // 백엔드에서 내려온 기존 값 설정
  const initialRole = (user.roleType || user.role || "USER").replace(
    "ROLE_",
    "",
  );
  const initialStatus = user.status || "ACTIVE";

  const [role, setRole] = useState(initialRole === "ADMIN" ? "ADMIN" : "USER");
  const [status, setStatus] = useState(initialStatus);

  return (
    <tr className="hover:bg-gray-50">
      <td className="p-3 font-semibold text-gray-800">
        <div>#{user.id}</div>
        <div className="text-[10px] text-gray-400">
          {user.email || "이메일 없음"}
        </div>
      </td>

      <td className="p-3 font-medium text-gray-700">
        {user.nickname || user.nickName || user.username || "-"}
      </td>

      <td className="p-3">
        <select
          value={role}
          onChange={(e) => setRole(e.target.value)}
          className="p-1.5 border rounded-lg bg-white font-medium text-xs border-gray-200 focus:outline-none focus:border-blue-500"
        >
          <option value="USER">USER</option>
          <option value="ADMIN">ADMIN</option>
        </select>
      </td>

      <td className="p-3">
        <select
          value={status}
          onChange={(e) => setStatus(e.target.value)}
          className="p-1.5 border rounded-lg bg-white font-medium text-xs border-gray-200 focus:outline-none focus:border-blue-500"
        >
          <option value="ACTIVE">ACTIVE</option>
          <option value="INACTIVE">INACTIVE</option>
          <option value="WITHDRAWN">WITHDRAWN</option>
        </select>
      </td>

      <td className="p-3 text-center">
        <button
          onClick={() => onUpdate(user.id, role, status)}
          className="bg-blue-600 text-white px-3 py-1.5 rounded-lg text-xs font-bold hover:bg-blue-700 transition-colors"
        >
          저장
        </button>
      </td>
    </tr>
  );
}
