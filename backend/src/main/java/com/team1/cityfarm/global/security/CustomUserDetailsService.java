package com.team1.cityfarm.global.security;

import com.team1.cityfarm.entity.User;
import com.team1.cityfarm.global.exception.CustomError;
import com.team1.cityfarm.global.exception.CustomException;
import com.team1.cityfarm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    // 기존 email 기준 조회 (로그인 시 사용)
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("사용자를 찾을 수 없습니다: " + email);
                    return new CustomException(CustomError.USER_NOT_FOUND);
                });
        return new CustomUserDetails(user);
    }

    // JWT Filter용: userId(Long) 기준 조회
    public CustomUserDetails loadUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("사용자를 찾을 수 없습니다. ID: " + userId);
                    return new CustomException(CustomError.USER_NOT_FOUND);
                });
        return new CustomUserDetails(user);
    }
}