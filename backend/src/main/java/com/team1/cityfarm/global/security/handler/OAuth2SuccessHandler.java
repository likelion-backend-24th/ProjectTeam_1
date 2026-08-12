package com.team1.cityfarm.global.security.handler;

import com.team1.cityfarm.entity.RefreshToken;
import com.team1.cityfarm.entity.RoleType;
import com.team1.cityfarm.entity.User;
import com.team1.cityfarm.global.exception.CustomError;
import com.team1.cityfarm.global.exception.CustomException;
import com.team1.cityfarm.global.security.jwt.JwtProvider;
import com.team1.cityfarm.global.security.oauth2.CustomOAuth2User;
import com.team1.cityfarm.repository.UserRepository;
import com.team1.cityfarm.service.RefreshTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

import static com.team1.cityfarm.global.exception.CustomError.USER_NOT_FOUND;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtProvider jwtProvider;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository; // 👈 필요하다면 유저 조회를 위해 주입 (아래 참고)

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        // 1. CustomOAuth2User에서 안전하게 원시값 추출
        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();
        Long userId = oAuth2User.getId();
        String email = oAuth2User.getEmail();
        RoleType roleType = RoleType.valueOf(oAuth2User.getRole()); // String을 RoleType으로 변환

        // 2. JWT 토큰 생성
        String accessToken = jwtProvider.createAccessToken(userId, email, roleType);
        String refreshToken = jwtProvider.createRefreshToken(userId);

        // 3. Refresh Token DB 저장 (refreshTokenService 내부에서 user 엔티티가 필요하다면 조회해서 넘김)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(CustomError.USER_NOT_FOUND));

        refreshTokenService.deleteAllByUserId(userId);
        refreshTokenService.addRefreshToken(
                RefreshToken.builder()
                        .user(user)
                        .token(refreshToken)
                        .build()
        );

        // 4. 프론트엔드로 토큰 전달하며 리다이렉트
        String targetUrl = UriComponentsBuilder.fromUriString("http://54.86.192.52/oauth/callback")
                .queryParam("accessToken", accessToken)
                .queryParam("refreshToken", refreshToken)
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}