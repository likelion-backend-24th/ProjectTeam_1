import {apiRequest} from "./client";

// 내 신청내역(원데이 클래스) 조회 = 확정된 것 중 날짜 빠른 순을 3개
export function getMyEnrollments(){
    return apiRequest("/api/enrollments/me");
}

// 구독 수강권으로 신청한 건 취소(수강권 자동 복구). 일반 결제 신청은 대신
// /api/payments/{paymentId}/cancel(lib/api/payment.js의 cancelPayment)을 써야 한다.
export function cancelEnrollmentByPass(enrollmentId) {
    return apiRequest(`/api/enrollments/${enrollmentId}/cancel`, { method: "POST" });
}