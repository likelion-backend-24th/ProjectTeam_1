package com.team1.cityfarm.service;

import com.team1.cityfarm.dto.ProfileRequestDto;
import com.team1.cityfarm.entity.User;
import com.team1.cityfarm.global.exception.CustomError;
import com.team1.cityfarm.global.exception.CustomException;
import com.team1.cityfarm.repository.UserRepository;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 기본적으로 읽기 전용으로 설정하여 조회 성능 최적화
public class ProfileService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(CustomError.USER_NOT_FOUND)); // 유저 없을 시 예외 처리
    }

    @Transactional
    public User updateProfile(Long userId, String nickname) throws CustomException {
        User user = getUser(userId);

        if (nickname == null || nickname.trim().isEmpty()) {
            throw new CustomException(CustomError.INVALID_INPUT_VALUE);
        }

        if (!user.getNickname().equals(nickname) && userRepository.existsByNickname(nickname)) {
            throw new CustomException(CustomError.INVALID_INPUT_VALUE);
        }

        user.setNickname(nickname);

        return user;
    }

    @Transactional(readOnly = true)
    public boolean checkCurrentPassword(Long userId, @NotBlank String currentPassword) {
        User user = getUser(userId);
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new CustomException(CustomError.AUTH_PASSWORD_VALID); // 비밀번호 틀리면 예외
        }
        return true;
    }

    @Transactional
    public void changePassword(Long userId, String newPassword) {
        User user = getUser(userId);
        user.setPassword(passwordEncoder.encode(newPassword)); // 암호화해서 저장
    }
}
