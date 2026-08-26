package com.team1.cityfarm.global.security.filter;

import com.team1.cityfarm.global.exception.CustomException;
import com.team1.cityfarm.global.security.jwt.JwtProvider;
import com.team1.cityfarm.global.security.user.CustomUserDetails;
import com.team1.cityfarm.global.security.user.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final CustomUserDetailsService customUserDetailsService;

    // 매 요청마다 userId로 DB를 조회해 탈퇴/정지(Status != ACTIVE) 계정의 기존 액세스 토큰이
    // 만료 전까지 계속 통하는 것을 막는다. 토큰 클레임만 믿고 통과시키던 이전 방식은
    // refresh token만 지워서는(로그아웃/탈퇴) 이미 발급된 access token을 무효화할 수 없었다.
    public JwtAuthenticationFilter(JwtProvider jwtProvider, CustomUserDetailsService customUserDetailsService) {
        this.jwtProvider = jwtProvider;
        this.customUserDetailsService = customUserDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String token = resolveToken(request);
        log.debug("[JWT 인증] uri: {}, tokenPresent: {}", request.getRequestURI(), token != null);

        if (token != null && jwtProvider.validateAccessToken(token)) {
            Long userId = jwtProvider.getUserId(token);

            try {
                // DB에서 최신 상태를 조회해 탈퇴/정지된 계정이면 인증시키지 않는다
                // (토큰 자체는 만료 전까지 유효해도, 계정 상태는 요청 시점 기준으로 판단).
                CustomUserDetails userDetails = customUserDetailsService.loadUserById(userId);

                if (userDetails.isEnabled()) {
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } else {
                    log.warn("[JWT 인증 거부] 비활성 계정의 토큰 사용 시도 - userId: {}", userId);
                }
            } catch (CustomException e) {
                log.warn("[JWT 인증 거부] 토큰의 userId에 해당하는 사용자를 찾을 수 없음 - userId: {}", userId);
            }
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}