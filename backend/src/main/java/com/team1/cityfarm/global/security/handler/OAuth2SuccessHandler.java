package com.team1.cityfarm.global.security.handler;

import com.team1.cityfarm.entity.RefreshToken;
import com.team1.cityfarm.entity.User;
import com.team1.cityfarm.global.security.jwt.JwtProvider;
import com.team1.cityfarm.repository.UserRepository;
import com.team1.cityfarm.service.RefreshTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        Map<String, Object> attributes = oAuth2User.getAttributes();

        // 1. 구글 프로필에서 이메일 추출
        String email = (String) attributes.get("email");

        // 2. DB에서 유저 조회
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        // 3. JWT 토큰 생성
        String accessToken = jwtProvider.createAccessToken(user.getId(), user.getEmail(), user.getRoleType());
        String refreshToken = jwtProvider.createRefreshToken(user.getId());

        // 4. Refresh Token DB 저장 (기존 토큰 삭제 후 새로 저장)
        refreshTokenService.deleteAllByUserId(user.getId());
        refreshTokenService.addRefreshToken(
                RefreshToken.builder()
                        .user(user)
                        .token(refreshToken)
                        .build()
        );

        // 5. 프론트엔드로 토큰을 전달하며 리다이렉트
        // (프론트엔드 주소에 맞춰 URL을 수정하세요)
        String targetUrl = UriComponentsBuilder.fromUriString("http://localhost:3000/oauth/callback")
                .queryParam("accessToken", accessToken)
                .queryParam("refreshToken", refreshToken)
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}