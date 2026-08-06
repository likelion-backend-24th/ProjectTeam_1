package com.team1.cityfarm.global.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtTokenizer {

    private final byte[] accessSecret;
    private final byte[] refreshSecret;
    private final long accessExpirationMs;
    private final long refreshExpirationMs;

    // token 초기값
    public JwtTokenizer(
            @Value("${jwt.secretKey}") String accessSecret,
            @Value("${jwt.refreshKey}") String refreshSecret,
            @Value("${jwt.access-expiration-ms}") long accessExpirationMs,
            @Value("${jwt.refresh-expiration-ms}") long refreshExpirationMs) {

        this.accessSecret = accessSecret.getBytes(StandardCharsets.UTF_8);
        this.refreshSecret = refreshSecret.getBytes(StandardCharsets.UTF_8);
        this.accessExpirationMs = accessExpirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    // 토큰 생성 및 파싱
    public String createAccessToken(
            Long id, String email, String nickname, String name) {
        return createToken(id, email, nickname, name, accessSecret, accessExpirationMs);
    }

    public String createRefreshToken(
            Long id, String email, String nickname, String name) {
        return createToken(id, email, nickname, name, refreshSecret, refreshExpirationMs);
    }

    private String createToken(
            Long id, String email, String nickname, String name, byte[] secretKey, Long expira) {

        Date now = new Date();
        Date expiration = new Date(now.getTime() + expira);

        return Jwts.builder()
                .subject(email)
                .claim("userId", id)
                .claim("nickname", nickname)
                .claim("name", name)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(getSigningKey(secretKey))
                .compact();
    }

    private SecretKey getSigningKey(byte[] secretKey) {
        return Keys.hmacShaKeyFor(secretKey);
    }

    private String removeBearerPrefix(String token) {
        if (token == null) {
            return null;
        }
        if (token.startsWith("Bearer ")) {
            return token.substring(7);
        }
        return token;
    }

    private Claims parseToken(String token, byte[] secret) {
        token = removeBearerPrefix(token);

        return Jwts.parser()
                .verifyWith(getSigningKey(secret))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Claims parseAccessToken(String accessToken) {
        return parseToken(accessToken, accessSecret);
    }

    public Claims parseRefreshToken(String refreshToken) {
        return parseToken(refreshToken, refreshSecret);
    }

    public Long getAccessTokenExpireCount() {
        return accessExpirationMs;
    }

    public Long getUserIdFromToken(String token) {
        token = removeBearerPrefix(token);
        Claims claims = parseToken(token, accessSecret);
        return claims.get("userId", Long.class);
    }

    public Long getRefreshToken() {
        return refreshExpirationMs;
    }
}