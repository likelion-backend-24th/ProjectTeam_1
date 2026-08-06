package com.team1.cityfarm.service;

import com.team1.cityfarm.dto.LoginRequestDto;
import com.team1.cityfarm.dto.LoginResponseDto;
import com.team1.cityfarm.dto.SignupRequestDto;
import com.team1.cityfarm.entity.RefreshToken;
import com.team1.cityfarm.entity.User;
import com.team1.cityfarm.global.exception.CustomError;
import com.team1.cityfarm.global.exception.CustomException;
import com.team1.cityfarm.global.util.JwtTokenizer;
import com.team1.cityfarm.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenizer jwtTokenizer;
    private final RefreshTokenService refreshTokenService;

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

        User user = userRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new CustomException(CustomError.AUTH_LOGIN_FAILED));

        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new CustomException(CustomError.AUTH_LOGIN_FAILED);
        }

        if (dto.getEmail() == null || dto.getEmail().isEmpty())
            throw new CustomException(CustomError.AUTH_EMAIL_REQUIRED);

        if (dto.getPassword() == null || dto.getPassword().isEmpty())
            throw new CustomException(CustomError.AUTH_PASSWORD_VALID);

        // 로그인 토큰
        String accessToken = jwtTokenizer.createAccessToken(
                user.getId(), user.getEmail(), user.getNickname(), user.getName());
        String refreshToken = jwtTokenizer.createRefreshToken(
                user.getId(), user.getEmail(), user.getNickname(), user.getName());

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

        Claims claims;
        try{
            claims = jwtTokenizer.parseRefreshToken(refreshToken);
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

        Long userId = claims.get("userId", Long.class);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(CustomError.USER_NOT_FOUND));

        String newAccessToken = jwtTokenizer.createAccessToken(
                user.getId(), user.getEmail(), user.getNickname(), user.getName());

        return LoginResponseDto.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)
                .build();
    }
}