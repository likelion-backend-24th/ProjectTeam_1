package com.team1.cityfarm.dto;

import com.team1.cityfarm.entity.ClassEnrollment;
import com.team1.cityfarm.entity.EnrollmentStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class EnrollmentApplicantResponseDto {

//    호스트용 - 내 클래스 신청자 목록

    private Long enrollmentId;
    private Long userId;
    private String userNickname;
    private EnrollmentStatus status;
    private LocalDateTime createdAt;

    public static EnrollmentApplicantResponseDto from(ClassEnrollment entity) {
        return EnrollmentApplicantResponseDto.builder()
                .enrollmentId(entity.getId())
                .userId(entity.getUser().getId())
                .userNickname(entity.getUser().getNickname())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
