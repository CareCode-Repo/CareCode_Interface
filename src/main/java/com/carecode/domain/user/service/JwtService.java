package com.carecode.domain.user.service;

import com.carecode.domain.user.dto.TokenDto;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 토큰 서비스
 * Access Token과 Refresh Token 생성, 검증, 갱신을 담당
 */
@Slf4j
@Service
public class JwtService {

    @Value("${jwt.secret:carecode-secret-key-for-jwt-token-generation}")
    private String secret;

    @Value("${jwt.access-token.expiration:3600000}") // 1시간
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token.expiration:2592000000}") // 30일
    private long refreshTokenExpiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    /**
     * Access Token 생성
     */
    public String generateAccessToken(String userId, String email, String role) {
        return generateToken(userId, email, role, accessTokenExpiration);
    }

    /**
     * Refresh Token 생성
     */
    public String generateRefreshToken(String userId, String email) {
        return generateToken(userId, email, null, refreshTokenExpiration);
    }

    /**
     * 토큰 생성
     */
    private String generateToken(String userId, String email, String role, long expiration) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("email", email);
        if (role != null) {
            claims.put("role", role);
        }

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 토큰에서 사용자 ID 추출
     */
    public String getUserIdFromToken(String token) {
        return getClaimFromToken(token, "userId", String.class);
    }

    /**
     * 토큰에서 이메일 추출
     */
    public String getEmailFromToken(String token) {
        return getClaimFromToken(token, "email", String.class);
    }

    /**
     * 토큰에서 역할 추출
     */
    public String getRoleFromToken(String token) {
        return getClaimFromToken(token, "role", String.class);
    }

    /**
     * 토큰에서 만료 시간 추출
     */
    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims.EXPIRATION, Date.class);
    }

    /**
     * 토큰에서 특정 클레임 추출
     */
    public <T> T getClaimFromToken(String token, String claimName, Class<T> requiredType) {
        final Claims claims = getAllClaimsFromToken(token);
        return claims.get(claimName, requiredType);
    }

    /**
     * 토큰에서 모든 클레임 추출
     */
    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 토큰 만료 여부 확인
     */
    public Boolean isTokenExpired(String token) {
        try {
            final Date expiration = getExpirationDateFromToken(token);
            return expiration.before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * 토큰 유효성 검증
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            log.error("JWT 토큰 검증 실패: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 토큰 검증 및 정보 추출
     */
    public TokenDto.TokenValidationResponse validateTokenAndExtractInfo(String token) {
        try {
            if (!validateToken(token)) {
                return TokenDto.TokenValidationResponse.builder()
                        .valid(false)
                        .message("유효하지 않은 토큰입니다.")
                        .build();
            }

            String userId = getUserIdFromToken(token);
            String email = getEmailFromToken(token);
            String role = getRoleFromToken(token);

            return TokenDto.TokenValidationResponse.builder()
                    .valid(true)
                    .userId(userId)
                    .email(email)
                    .role(role)
                    .message("토큰이 유효합니다.")
                    .build();
        } catch (Exception e) {
            log.error("토큰 검증 중 오류 발생: {}", e.getMessage());
            return TokenDto.TokenValidationResponse.builder()
                    .valid(false)
                    .message("토큰 검증 중 오류가 발생했습니다.")
                    .build();
        }
    }

    /**
     * 토큰 갱신
     */
    public TokenDto refreshTokens(String refreshToken) {
        try {
            if (!validateToken(refreshToken)) {
                throw new IllegalArgumentException("유효하지 않은 Refresh Token입니다.");
            }

            String userId = getUserIdFromToken(refreshToken);
            String email = getEmailFromToken(refreshToken);
            String role = getRoleFromToken(refreshToken);

            // 새로운 Access Token과 Refresh Token 생성
            String newAccessToken = generateAccessToken(userId, email, role != null ? role : "PARENT");
            String newRefreshToken = generateRefreshToken(userId, email);

            return TokenDto.builder()
                    .accessToken(newAccessToken)
                    .refreshToken(newRefreshToken)
                    .tokenType("Bearer")
                    .expiresIn(accessTokenExpiration)
                    .userId(userId)
                    .email(email)
                    .role(role != null ? role : "PARENT")
                    .success(true)
                    .message("토큰 갱신 성공")
                    .build();
        } catch (Exception e) {
            log.error("토큰 갱신 실패: {}", e.getMessage());
            throw new RuntimeException("토큰 갱신에 실패했습니다.", e);
        }
    }

    /**
     * 토큰에서 Authorization 헤더 추출
     */
    public String extractTokenFromAuthorizationHeader(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        }
        return null;
    }
} 