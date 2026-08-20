package com.team1.cityfarm.dto;

import com.team1.cityfarm.entity.ClassEnrollment;
import com.team1.cityfarm.entity.EnrollmentStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class MyEnrollmentResponseDto {

//    마이페이지 - 내가 신청한 클래스

    private Long enrollmentId;
    private Long classId;
    private Long orderId;
    private String classTitle;
    private LocalDateTime classDate;
    private String classLocation;
    private EnrollmentStatus status;
    private LocalDateTime createdAt;

    public static MyEnrollmentResponseDto from(ClassEnrollment entity) {
        return MyEnrollmentResponseDto.builder()
                .enrollmentId(entity.getId())
                .classId(entity.getOneDayClass().getId())
                .orderId(entity.getOrderId())
                .classTitle(entity.getOneDayClass().getTitle())
                .classDate(entity.getOneDayClass().getDate())
                .classLocation(entity.getOneDayClass().getLocation())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
