package com.team1.cityfarm.global.security.jwt;

import com.team1.cityfarm.entity.RoleType;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;

@Component
public class JwtProvider {

    private final SecretKey accessSecretKey;
    private final SecretKey refreshSecretKey;
    private final long accessExpirationMs;
    private final long refreshExpirationMs;

    public JwtProvider(
            @Value("${jwt.secretKey}") String accessSecret,
            @Value("${jwt.refreshKey}") String refreshSecret,
            @Value("${jwt.access-expiration-ms}") long accessExpirationMs,
            @Value("${jwt.refresh-expiration-ms}") long refreshExpirationMs
    ) {
        this.accessSecretKey = Keys.hmacShaKeyFor(accessSecret.getBytes(StandardCharsets.UTF_8));
        this.refreshSecretKey = Keys.hmacShaKeyFor(refreshSecret.getBytes(StandardCharsets.UTF_8));
        this.accessExpirationMs = accessExpirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    // ================= Access Token 관련 =================

    // Access Token 생성 (email과 roleType을 Claim에 저장)
    public String createAccessToken(Long userId, String email, RoleType roleType) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + accessExpirationMs);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("email", email)
                .claim("role", roleType.name())
                .issuedAt(now)
                .expiration(validity)
                .signWith(accessSecretKey)
                .compact();
    }

    // Access Token 검증
    public boolean validateAccessToken(String token) {
        return validateToken(token, accessSecretKey);
    }

    // Access Token에서 Claims 추출
    private Claims getAccessClaims(String token) {
        return Jwts.parser()
                .verifyWith(accessSecretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // Access Token에서 userId 추출
    public Long getUserId(String token) {
        return Long.parseLong(getAccessClaims(token).getSubject());
    }

    // Access Token에서 email 추출 (추가)
    public String getEmail(String token) {
        return getAccessClaims(token).get("email", String.class);
    }

    // Access Token에서 role 추출 (추가)
    public String getRole(String token) {
        return getAccessClaims(token).get("role", String.class);
    }

    // SecurityContext 등록용 Authentication 생성
    public Authentication getAuthentication(String token) {
        Long userId = getUserId(token);
        return new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList());
    }

    // ================= Refresh Token 관련 =================

    // Refresh Token 생성
    public String createRefreshToken(Long userId) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + refreshExpirationMs);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(now)
                .expiration(validity)
                .signWith(refreshSecretKey)
                .compact();
    }

    // Refresh Token 검증
    public boolean validateRefreshToken(String token) {
        return validateToken(token, refreshSecretKey);
    }

    // Refresh Token에서 userId 추출
    public Long getUserIdFromRefreshToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(refreshSecretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return Long.parseLong(claims.getSubject());
    }

    // ================= 공통 메서드 =================

    private boolean validateToken(String token, SecretKey key) {
        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}