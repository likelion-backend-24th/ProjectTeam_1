package com.team1.cityfarm.service;

import com.team1.cityfarm.dto.OneDayClassRequestDto;
import com.team1.cityfarm.dto.OneDayClassResponseDto;
import com.team1.cityfarm.dto.OneDayClassSummaryDto;
import com.team1.cityfarm.entity.OneDayClass;
import com.team1.cityfarm.entity.RoleType;
import com.team1.cityfarm.entity.User;
import com.team1.cityfarm.global.exception.CustomError;
import com.team1.cityfarm.global.exception.CustomException;
import com.team1.cityfarm.repository.OneDayClassRepository;
import com.team1.cityfarm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OneDayClassService {
    private final OneDayClassRepository oneDayClassRepository;
    private final UserRepository userRepository;

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
        return OneDayClassResponseDto.from(saved);
    }

    //    목록조회
    public Page<OneDayClassSummaryDto> getClassList(Pageable pageable) {
        return oneDayClassRepository.findAll(pageable)
                .map(OneDayClassSummaryDto::from);
    }

    //    상세조회
    public OneDayClassResponseDto getClassDetail(Long classId) {
        OneDayClass oneDayClass = oneDayClassRepository.findById(classId)
                .orElseThrow(() -> new CustomException(CustomError.ONE_DAY_CLASS_NOT_FOUND));

        return OneDayClassResponseDto.from(oneDayClass);
    }

}
