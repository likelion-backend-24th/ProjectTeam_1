package com.team1.cityfarm.service;

import com.team1.cityfarm.dto.ProfileRequestDto;
import com.team1.cityfarm.entity.User;
import com.team1.cityfarm.global.exception.CustomError;
import com.team1.cityfarm.global.exception.CustomException;
import com.team1.cityfarm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 기본적으로 읽기 전용으로 설정하여 조회 성능 최적화
public class ProfileService {
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(CustomError.USER_NOT_FOUND)); // 유저 없을 시 예외 처리
    }

    @Transactional
    public User updateProfile(Long userId, String nickName) throws CustomException {
        User user = getUser(userId);

        if (nickName == null || nickName.trim().isEmpty()) {
            throw new CustomException(CustomError.INVALID_INPUT_VALUE);
        }

        if (!user.getNickname().equals(nickName) && userRepository.existsByNickname(nickName)) {
            throw new CustomException(CustomError.INVALID_INPUT_VALUE);
        }

        user.setNickname(nickName);

        return user;
    }
}
