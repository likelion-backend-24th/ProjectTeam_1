package com.team1.cityfarm.service;

import com.team1.cityfarm.repository.OneDayClassRepository;
import com.team1.cityfarm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OneDayClassService {
    private final OneDayClassRepository oneDayClassRepository;
    private final UserRepository userRepository;

//    등록

//    목록조회

//    상세조회

}
