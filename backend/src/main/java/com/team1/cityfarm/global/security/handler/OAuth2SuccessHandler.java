package com.team1.cityfarm.global.security.handler;

import com.team1.cityfarm.entity.RefreshToken;
import com.team1.cityfarm.entity.User;
import com.team1.cityfarm.global.security.jwt.JwtProvider;
import com.team1.cityfarm.global.security.oauth2.CustomOAuth2User;
import com.team1.cityfarm.service.RefreshTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtProvider jwtProvider;
    private final RefreshTokenService refreshTokenService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        // 1. CustomOAuth2User에서 User 엔티티 직접 추출 (소셜 3사 공통 안전)
        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();
        User user = oAuth2User.getUser();

        // 2. JWT 토큰 생성
        String accessToken = jwtProvider.createAccessToken(user.getId(), user.getEmail(), user.getRoleType());
        String refreshToken = jwtProvider.createRefreshToken(user.getId());

        // 3. Refresh Token DB 저장
        refreshTokenService.deleteAllByUserId(user.getId());
        refreshTokenService.addRefreshToken(
                RefreshToken.builder()
                        .user(user)
                        .token(refreshToken)
                        .build()
        );

        // 4. 프론트엔드로 토큰을 전달하며 리다이렉트 (EC2 주소 / 프론트 엔드포인트)
        String targetUrl = UriComponentsBuilder.fromUriString("http://54.86.192.52/oauth/callback")
                .queryParam("accessToken", accessToken)
                .queryParam("refreshToken", refreshToken)
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}