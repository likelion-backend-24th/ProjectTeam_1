package com.team1.cityfarm.global.security.filter;

import com.team1.cityfarm.entity.RoleType;
import com.team1.cityfarm.global.security.jwt.JwtProvider;
import com.team1.cityfarm.global.security.user.CustomUserDetails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;

    // customUserDetailsService 제거 완료
    public JwtAuthenticationFilter(JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        System.out.println("=========================================");
        System.out.println("[Request URI] " + request.getRequestURI());
        System.out.println("[Auth Header] " + request.getHeader("Authorization"));

        String token = resolveToken(request);
        System.out.println("[Extracted Token] " + token);

        if (token != null) {
            System.out.println("[Token Valid] " + jwtProvider.validateAccessToken(token));
        }
        System.out.println("=========================================");

//        String token = resolveToken(request);

        if (token != null && jwtProvider.validateAccessToken(token)) {
            // 1. 토큰 Claim에서 회원 정보 추출
            Long userId = jwtProvider.getUserId(token);
            String email = jwtProvider.getEmail(token);
            String roleStr = jwtProvider.getRole(token); // "USER" 또는 "ADMIN"
            RoleType roleType = RoleType.valueOf(roleStr);

            // 2. DB 조회 없이 추출한 정보로 경량 CustomUserDetails 생성
            CustomUserDetails userDetails = new CustomUserDetails(userId, email, roleType);

            // 3. SecurityContext에 Authentication 객체 저장
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

            SecurityContextHolder.getContext().setAuthentication(authentication);
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