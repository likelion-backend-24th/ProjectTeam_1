package com.team1.cityfarm.controller;

import com.team1.cityfarm.dto.EnrollmentApplicantResponseDto;
import com.team1.cityfarm.dto.MyEnrollmentResponseDto;
import com.team1.cityfarm.global.response.ApiResponse;
import com.team1.cityfarm.global.security.user.CustomUserDetails;
import com.team1.cityfarm.service.ClassEnrollmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping
public class ClassEnrollmentController {

    private final ClassEnrollmentService classEnrollmentService;

//    마이페이지
    @Operation(summary = "내 신청내역 조회",
    description = "로그인한 사용자가 신청한 클래스 목록 조회.",
            security = @SecurityRequirement(name = "BearerAuth"))
    @GetMapping("/api/enrollments/me")
    public ApiResponse<List<MyEnrollmentResponseDto>> getMyEnrollment(@AuthenticationPrincipal CustomUserDetails customUserDetails){

        Long userId = customUserDetails.getUserId();

        return ApiResponse.success("내 신청내역 조회 성공", classEnrollmentService.getMyEnrollment(userId));

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
