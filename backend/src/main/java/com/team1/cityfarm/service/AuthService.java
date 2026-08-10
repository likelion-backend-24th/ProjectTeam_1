package com.team1.cityfarm.service;

import com.team1.cityfarm.dto.LoginRequestDto;
import com.team1.cityfarm.dto.LoginResponseDto;
import com.team1.cityfarm.dto.SignupRequestDto;
import com.team1.cityfarm.entity.RefreshToken;
import com.team1.cityfarm.entity.User;
import com.team1.cityfarm.global.exception.CustomError;
import com.team1.cityfarm.global.exception.CustomException;
import com.team1.cityfarm.global.security.JwtProvider;
import com.team1.cityfarm.repository.RefreshTokenRepository;
import com.team1.cityfarm.repository.UserRepository;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@Transactional
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final RefreshTokenService refreshTokenService;
    private final RefreshTokenRepository refreshTokenRepository;

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
    public LoginResponseDto login(LoginRequestDto dto) {

        if (dto.getEmail() == null || dto.getEmail().isEmpty())
            throw new CustomException(CustomError.AUTH_EMAIL_REQUIRED);

        if (dto.getPassword() == null || dto.getPassword().isEmpty())
            throw new CustomException(CustomError.AUTH_PASSWORD_VALID);

        User user = userRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new CustomException(CustomError.AUTH_LOGIN_FAILED));

        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new CustomException(CustomError.AUTH_LOGIN_FAILED);
        }

        String accessToken = jwtProvider.createAccessToken(user.getId(), user.getEmail(), user.getRoleType());
        String refreshToken = jwtProvider.createRefreshToken(user.getId());
        refreshTokenService.deleteAllByUserId(user.getId());

        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .user(user)
                .token(refreshToken)
                .build();
        refreshTokenService.addRefreshToken(refreshTokenEntity);

        return new LoginResponseDto(accessToken, refreshToken);
    }

    // 토큰 재발급
    @Transactional
    public LoginResponseDto reissueAccessToken(String refreshToken){
        if (refreshToken == null){
            throw new CustomException(CustomError.TOKEN_NOT_FOUND);
        }

        Long userId;
        try{
            userId = jwtProvider.getUserIdFromRefreshToken(refreshToken);
        } catch (ExpiredJwtException e){
            throw new CustomException(CustomError.TOKEN_EXPIRED);
        } catch (JwtException | IllegalArgumentException e){
            throw new CustomException(CustomError.TOKEN_INVALID);
        }

        RefreshToken dbToken = refreshTokenService.findRefreshToken(refreshToken)
                .orElseThrow(() -> new CustomException(CustomError.TOKEN_INVALID));
        if (!refreshToken.equals(dbToken.getToken())){
            throw new CustomException(CustomError.TOKEN_INVALID);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(CustomError.USER_NOT_FOUND));

        String newAccessToken = jwtProvider.createAccessToken(user.getId(), user.getEmail(), user.getRoleType());

        return new LoginResponseDto(newAccessToken, refreshToken);
    }

    //로그아웃
    @Transactional
    public void logout(Long userId) {
        // DB에서 해당 유저의 Refresh Token 삭제
        refreshTokenService.deleteAllByUserId(userId);
    }

    @Transactional
    public void withdraw(Long userId) {
        // 1. 회원 존재 여부 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(CustomError.USER_NOT_FOUND));

        // 2. Redis 등에 저장된 RefreshToken 삭제
        refreshTokenRepository.deleteByUserId(userId);

        // 3. 회원 탈퇴 처리 (하드 삭제)
        userRepository.delete(user); // 또는 user.withdraw(); (소프트 삭제 시)
    }
}