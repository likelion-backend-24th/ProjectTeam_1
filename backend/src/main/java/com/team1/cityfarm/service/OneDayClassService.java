package com.team1.cityfarm.service;

import com.team1.cityfarm.dto.OneDayClassDescriptionUpdateDto;
import com.team1.cityfarm.dto.OneDayClassRequestDto;
import com.team1.cityfarm.dto.OneDayClassResponseDto;
import com.team1.cityfarm.dto.OneDayClassSummaryDto;
import com.team1.cityfarm.entity.*;
import com.team1.cityfarm.global.exception.CustomError;
import com.team1.cityfarm.global.exception.CustomException;
import com.team1.cityfarm.repository.OneDayClassRepository;
import com.team1.cityfarm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OneDayClassService {
    private final OneDayClassRepository oneDayClassRepository;
    private final UserRepository userRepository;
    private final ClassEnrollmentService classEnrollmentService;

    //    등록
    @Transactional
    public OneDayClassResponseDto createOneDayClass(Long hostId, OneDayClassRequestDto requestDto) {
        User host = userRepository.findById(hostId).orElseThrow(() -> new CustomException(CustomError.USER_NOT_FOUND));

        if (host.getRoleType() != RoleType.HOST) {
            throw new CustomException(CustomError.NOT_HOST_ROLE);
        }
        OneDayClass oneDayClass = OneDayClass.builder()
                .host(host)
                .title(requestDto.getTitle())
                .description(requestDto.getDescription())
                .date(requestDto.getDate())
                .location(requestDto.getLocation())
                .capacity(requestDto.getCapacity())
                .price(requestDto.getPrice())
                .build();

        OneDayClass saved = oneDayClassRepository.save(oneDayClass);
        return OneDayClassResponseDto.from(saved, 0);
    }

    //    목록조회 (취소된 클래스는 제외)
    public Page<OneDayClassSummaryDto> getClassList(Pageable pageable) {
        return oneDayClassRepository.findByStatus(ClassStatus.OPEN, pageable)
                .map(cls -> OneDayClassSummaryDto.from(cls, classEnrollmentService.getEnrolledCount(cls.getId())));
    }

    //    상세조회
    public OneDayClassResponseDto getClassDetail(Long classId) {
        OneDayClass oneDayClass = oneDayClassRepository.findById(classId)
                .orElseThrow(() -> new CustomException(CustomError.ONE_DAY_CLASS_NOT_FOUND));

        return OneDayClassResponseDto.from(oneDayClass, classEnrollmentService.getEnrolledCount(classId));
    }

    //    수업 설명 수정
    @Transactional
    public OneDayClassResponseDto updateDescription(Long classId, Long hostId, OneDayClassDescriptionUpdateDto requestDto) {
        OneDayClass oneDayClass = oneDayClassRepository.findById(classId)
                .orElseThrow(() -> new CustomException(CustomError.ONE_DAY_CLASS_NOT_FOUND));

        if (!oneDayClass.getHost().getId().equals(hostId)) {
            throw new CustomException(CustomError.CLASS_NOT_OWNER);
        }

        oneDayClass.setDescription(requestDto.getDescription());

        return OneDayClassResponseDto.from(oneDayClass, classEnrollmentService.getEnrolledCount(classId));
    }

    //    호스트 관리 화면 - 내가 개설한 클래스 개수
    public long countMyClasses(Long hostId) {
        return oneDayClassRepository.countByHost_Id(hostId);
    }

    //    클래스 취소
    @Transactional
    public List<ClassEnrollment> cancelClass(Long classId, Long hostId) {
        OneDayClass oneDayClass = oneDayClassRepository.findById(classId)
                .orElseThrow(() -> new CustomException(CustomError.ONE_DAY_CLASS_NOT_FOUND));

        if (!oneDayClass.getHost().getId().equals(hostId)) {
            throw new CustomException(CustomError.CLASS_NOT_OWNER);
        }

        if (oneDayClass.getStatus() == ClassStatus.CANCELLED) {
            return List.of(); // 멱등 처리
        }

        oneDayClass.setStatus(ClassStatus.CANCELLED);

        return classEnrollmentService.cancelAllEnrollmentsForClass(classId);
    }

}
