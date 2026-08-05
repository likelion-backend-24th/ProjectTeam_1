package com.team1.cityfarm.service;

import com.team1.cityfarm.dto.LoginRequestDto;
import com.team1.cityfarm.dto.SignupRequestDto;
import com.team1.cityfarm.entity.User;
import com.team1.cityfarm.global.exception.CustomError;
import com.team1.cityfarm.global.exception.CustomException;
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
            throw new CustomException(CustomError.AUTH_DUPLICATED_EMAIL);
        }

        if (!dto.getPassword().equals(dto.getPasswordConfirm())){
            throw new CustomException(CustomError.AUTH_PASSWORD_VALID);
        }

        if (userRepository.findByNickname(dto.getNickname()).isPresent()){
            throw new CustomException(CustomError.AUTH_DUPLICATED_NICKNAME);
        }

        User user = User.builder()
                .email(dto.getEmail())
                .password(passwordEncoder.encode(dto.getPassword()))
                .nickname(dto.getNickname())
                .name(dto.getName())
                .build();

        userRepository.save(user);
    }

    // 로그인
    public void login(LoginRequestDto dto){

        User user = userRepository.findByEmail(dto.getEmail())
                .orElseThrow(()-> new CustomException(CustomError.AUTH_LOGIN_FAILED));

        if (!passwordEncoder.matches(dto.getPassword(),user.getPassword())){
            throw new CustomException(CustomError.AUTH_LOGIN_FAILED);
        }

        if (dto.getEmail() == null || dto.getEmail().isEmpty())
           throw new CustomException(CustomError.AUTH_EMAIL_REQUIRED);

        if (dto.getPassword() == null || dto.getPassword().isEmpty())
            throw new CustomException(CustomError.AUTH_PASSWORD_VALID);

    }


}
