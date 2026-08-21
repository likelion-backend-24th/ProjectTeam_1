'use client';

import { useState, useEffect } from 'react';
import { getHostSettlements } from '@/lib/api/settlement';

export default function HostSettlementPage() {
  const [settlements, setSettlements] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchSettlements = async () => {
      try {
        // 공통 apiRequest를 타는 함수 호출
        const data = await getHostSettlements();
        setSettlements(data);
      } catch (err) {
        setError(err.message || '정산 내역을 불러오는데 실패했습니다.');
      } finally {
        setLoading(false);
      }
    };

    fetchSettlements();
  }, []);

  if (loading) {
    return (
      <div className="flex justify-center items-center min-h-[50vh]">
        <p className="text-gray-500 text-lg">정산 내역을 불러오는 중...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex justify-center items-center min-h-[50vh]">
        <p className="text-red-500 text-lg">에러 발생: {error}</p>
      </div>
    );
  }

  return (
    <div className="max-w-5xl mx-auto px-4 py-8">
      <h1 className="text-2xl font-bold mb-6 text-gray-900">내 정산 내역</h1>

      <div className="bg-white shadow-md rounded-lg overflow-hidden border border-gray-200">
        <table className="w-full text-left border-collapse">
          <thead className="bg-gray-50 text-gray-700 uppercase text-xs tracking-wider">
            <tr>
              <th className="py-3 px-6">정산 번호</th>
              <th className="py-3 px-6">정산 금액</th>
              <th className="py-3 px-6">상태</th>
              <th className="py-3 px-6">생성 일시</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-200 text-sm text-gray-600">
            {settlements.length === 0 ? (
              <tr>
                <td colSpan={4} className="py-6 text-center text-gray-400">
                  조회된 정산 내역이 없습니다.
                </td>
              </tr>
            ) : (
              settlements.map((item) => (
                <tr key={item.id} className="hover:bg-gray-50">
                  <td className="py-4 px-6 font-medium text-gray-900">{item.id}</td>
                  <td className="py-4 px-6 font-semibold text-blue-600">
                    {item.settlementAmount.toLocaleString()}원
                  </td>
                  <td className="py-4 px-6">
                    <span
                      className={`px-2.5 py-1 rounded-full text-xs font-semibold ${
                        item.status === 'COMPLETED'
                          ? 'bg-green-100 text-green-800'
                          : 'bg-yellow-100 text-yellow-800'
                      }`}
                    >
                      {item.status}
                    </span>
                  </td>
                  <td className="py-4 px-6">{item.createdAt}</td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}