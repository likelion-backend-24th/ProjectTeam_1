import {apiRequest} from "./client";

// 내 신청내역(원데이 클래스) 조회 = 확정된 것 중 날짜 빠른 순을 3개
export function getMyEnrollments(){
    return apiRequest("/api/enrollments/me");
}