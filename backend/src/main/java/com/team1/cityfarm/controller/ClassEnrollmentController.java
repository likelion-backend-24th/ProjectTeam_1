package com.team1.cityfarm.controller;

import com.team1.cityfarm.dto.EnrollmentApplicantResponseDto;
import com.team1.cityfarm.dto.MyEnrollmentResponseDto;
import com.team1.cityfarm.global.response.ApiResponse;
import com.team1.cityfarm.global.security.user.CustomUserDetails;
import com.team1.cityfarm.service.ClassEnrollmentService;
import com.team1.cityfarm.service.SubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping
public class ClassEnrollmentController {

    private final ClassEnrollmentService classEnrollmentService;
    private final SubscriptionService subscriptionService;

//    마이페이지
    @Operation(summary = "마이페이지 - 다가오는 클래스 조회",
    description = "확정된 신청 중 현재 시각 이후의 클래스를 날짜/시간이 빠른 순으로 설정",
            security = @SecurityRequirement(name = "BearerAuth"))
    @GetMapping("/api/enrollments/me")
    public ApiResponse<List<MyEnrollmentResponseDto>> getMyEnrollment(@AuthenticationPrincipal CustomUserDetails customUserDetails){

        Long userId = customUserDetails.getUserId();

        return ApiResponse.success("내 신청내역 조회 성공", classEnrollmentService.getMyEnrollment(userId));

    }

//    구독 수강권으로 클래스 신청 (결제 없이 즉시 확정)
    @Operation(summary = "구독 수강권으로 클래스 신청",
            description = "보유한 구독 수강권 1개를 차감해 결제 없이 즉시 신청을 확정한다.",
            security = @SecurityRequirement(name = "BearerAuth"))
    @PostMapping("/api/onedayclass/{classId}/enroll-with-pass")
    public ApiResponse<MyEnrollmentResponseDto> enrollWithPass(
            @PathVariable Long classId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {

        Long userId = customUserDetails.getUserId();

        return ApiResponse.success("수강권으로 신청 완료", subscriptionService.enrollClassWithPass(userId, classId));
    }

//    구독 수강권 개인취소 (수강권 복구 포함)
    @Operation(summary = "구독 수강권 신청 개인 취소",
    description = "본인 구독 수강권으로 신청한 클래스를 취소, 사용 수강권을 복구한다.",
    security = @SecurityRequirement(name = "BearerAuth"))
    @PostMapping("/api/enrollments/{enrollmentId}/cancel")
    public ApiResponse<Void> cancelMyEnrollment(@PathVariable Long enrollmentId,
                                                @AuthenticationPrincipal CustomUserDetails customUserDetails){
        Long userId = customUserDetails.getUserId();

        classEnrollmentService.cancelEnrollmentByPass(enrollmentId, userId);
        subscriptionService.restorePassByEnrollmentId(enrollmentId);

        return ApiResponse.success("신청 취소 완료");
    }

//    호스트
    @Operation(summary = "내 클래스 신청자 목록 조회",
            description = "호스트(본인)이 개설한 클래스의 신청자 목록 조회."
    ,security = @SecurityRequirement(name = "BearerAuth"))
    @GetMapping("/api/onedayclass/{classId}/applicants")
    public ApiResponse<List<EnrollmentApplicantResponseDto>>getApplicants(
            @PathVariable Long classId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails){

        Long hostId = customUserDetails.getUserId();

        return ApiResponse.success("신청목록 조회 성공", classEnrollmentService.getApplicant(classId,hostId));

    }

}
