package com.team1.cityfarm.service;

import com.team1.cityfarm.dto.UserRequestDto;
import com.team1.cityfarm.dto.UserResponseDto;
import com.team1.cityfarm.entity.User;
import com.team1.cityfarm.global.exception.CustomError;
import com.team1.cityfarm.global.exception.CustomException;
import com.team1.cityfarm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminService {
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<UserResponseDto> getAllUsers(Pageable pageable){
        return userRepository.findAll(pageable).map(UserResponseDto::from);
    }

    @Transactional
    public UserResponseDto modifyUser(Long id, UserRequestDto dto){
        //id로 user 조회
        User loadUser = userRepository.findById(id).orElseThrow(()->new CustomException(CustomError.USER_NOT_FOUND));
        //활동 상태 변경
        loadUser.updateStatus(dto.getStatus());
        return UserResponseDto.from(loadUser);
    }

}
