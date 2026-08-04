package com.team1.cityfarm.service;

import com.team1.cityfarm.dto.SignupRequestDto;
import com.team1.cityfarm.entity.User;
import com.team1.cityfarm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;


    // 회원가입
    public void signUp(SignupRequestDto dto){
        if (userRepository.findByEmail(dto.getEmail()).isPresent()){
            throw new IllegalArgumentException("이미 가입된 이메일입니다.");
        }

        if (!dto.getPassword().equals(dto.getPasswordConfirm())){
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        if (userRepository.findByEmail(dto.getNickname()).isPresent()){
            throw new IllegalArgumentException("중복된 닉네임이 있습니다.");
        }

        User user = User.builder()
                .email(dto.getEmail())
                .password(passwordEncoder.encode(dto.getPassword()))
                .nickname(dto.getNickname())
                .name(dto.getName())
                .build();

        userRepository.save(user);
    }
}
