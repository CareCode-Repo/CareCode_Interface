package com.carecode.domain.user.service;

import com.carecode.core.exception.BusinessException;
import com.carecode.core.exception.ErrorCode;
import com.carecode.domain.user.dto.response.TokenDto;
import com.carecode.domain.user.dto.response.TokenValidationResponse;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * JWT 토큰 서비스
 * Access Token과 Refresh Token 생성, 검증, 갱신을 담당
 */
@Slf4j
@Service
public class JwtService {

    /** 토큰 종류를 구분하는 클레임. Access Token 을 Refresh 로, 혹은 그 반대로 쓰는 것을 막습니다. */
    public static final String CLAIM_TOKEN_TYPE = "typ";
    public static final String TOKEN_TYPE_ACCESS = "access";
    public static final String TOKEN_TYPE_REFRESH = "refresh";

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token.expiration:3600000}") // 1시간
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token.expiration:2592000000}") // 30일
    private long refreshTokenExpiration;

    @Value("${jwt.issuer:carecode}")
    private String issuer;

    public long getAccessTokenExpirationMs() {
        return accessTokenExpiration;
    }

    public long getRefreshTokenExpirationMs() {
        return refreshTokenExpiration;
    }

    @PostConstruct
    public void validateJwtSecret() {
        if (secret == null || secret.trim().isEmpty()) {
            throw new IllegalStateException("JWT secret must be configured.");
        }
        if (secret.length() < 32) {
            throw new IllegalStateException("JWT secret must be at least 32 characters.");
        }
    }

    private SecretKey getSigningKey() {
        // 플랫폼 기본 인코딩에 따라 키가 달라지지 않도록 UTF-8 을 명시합니다.
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }


    // Access Token 생성

    public String generateAccessToken(String userId, String email, String role) {
        return generateToken(TOKEN_TYPE_ACCESS, userId, email, role, null, accessTokenExpiration);
    }


    // Access Token 생성 (name 포함)

    public String generateAccessToken(String userId, String email, String role, String name) {
        return generateToken(TOKEN_TYPE_ACCESS, userId, email, role, name, accessTokenExpiration);
    }


    // Refresh Token 생성

    public String generateRefreshToken(String userId, String email) {
        return generateToken(TOKEN_TYPE_REFRESH, userId, email, null, null, refreshTokenExpiration);
    }


    // 토큰 생성

    private String generateToken(String tokenType, String userId, String email, String role, String name, long expiration) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_TOKEN_TYPE, tokenType);
        claims.put("userId", userId);
        claims.put("email", email);
        if (role != null) {
            claims.put("role", role);
        }
        if (name != null) {
            claims.put("name", name);
        }

        return Jwts.builder()
                .setClaims(claims)
                .setId(UUID.randomUUID().toString())
                .setIssuer(issuer)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }


    // 토큰에서 종류(access/refresh) 추출

    public String getTokenType(String token) {
        return getClaimFromToken(token, CLAIM_TOKEN_TYPE, String.class);
    }


    // 토큰에서 사용자 ID 추출

    public String getUserIdFromToken(String token) {
        return getClaimFromToken(token, "userId", String.class);
    }


    // 토큰에서 이메일 추출

    public String getEmailFromToken(String token) {
        return getClaimFromToken(token, "email", String.class);
    }
    

    // 토큰에서 이메일 추출 (별칭 메서드)

    public String extractEmailFromToken(String token) {
        return getEmailFromToken(token);
    }


    // 토큰에서 역할 추출

    public String getRoleFromToken(String token) {
        return getClaimFromToken(token, "role", String.class);
    }


    // 토큰에서 이름 추출

    public String getNameFromToken(String token) {
        return getClaimFromToken(token, "name", String.class);
    }


    // 토큰에서 만료 시간 추출

    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims.EXPIRATION, Date.class);
    }


    // 토큰에서 특정 클레임 추출

    public <T> T getClaimFromToken(String token, String claimName, Class<T> requiredType) {
        final Claims claims = getAllClaimsFromToken(token);
        return claims.get(claimName, requiredType);
    }


    // 토큰에서 모든 클레임 추출

    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }


    // 토큰 만료 여부 확인

    public Boolean isTokenExpired(String token) {
        try {
            final Date expiration = getExpirationDateFromToken(token);
            return expiration.before(new Date());
        } catch (Exception e) {
            return true;
        }
    }


    // 토큰 유효성 검증

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .requireIssuer(issuer)
                    .build()
                    .parseClaimsJws(token);
            return !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("JWT 토큰 검증 실패: {}", e.getMessage());
            return false;
        }
    }


    // Access Token 전용 검증 - typ=access 인 토큰만 통과시킵니다.

    public boolean validateAccessToken(String token) {
        return validateTokenOfType(token, TOKEN_TYPE_ACCESS);
    }


    // Refresh Token 전용 검증 - typ=refresh 인 토큰만 통과시킵니다.

    public boolean validateRefreshToken(String token) {
        return validateTokenOfType(token, TOKEN_TYPE_REFRESH);
    }

    private boolean validateTokenOfType(String token, String expectedType) {
        if (!validateToken(token)) {
            return false;
        }
        try {
            String actualType = getTokenType(token);
            if (!expectedType.equals(actualType)) {
                log.warn("JWT 토큰 종류 불일치: 기대={}, 실제={}", expectedType, actualType);
                return false;
            }
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("JWT 토큰 종류 확인 실패: {}", e.getMessage());
            return false;
        }
    }


    // 토큰 검증 및 정보 추출

    public TokenValidationResponse validateTokenAndExtractInfo(String token) {
        try {
            if (!validateToken(token)) {
                return TokenValidationResponse.builder()
                        .valid(false)
                        .message("유효하지 않은 토큰입니다.")
                        .build();
            }

            String userId = getUserIdFromToken(token);
            String email = getEmailFromToken(token);
            String role = getRoleFromToken(token);

            return TokenValidationResponse.builder()
                    .valid(true)
                    .userId(userId)
                    .email(email)
                    .role(role)
                    .message("토큰이 유효합니다.")
                    .build();
        } catch (Exception e) {
            log.warn("토큰 검증 중 오류 발생: {}", e.getMessage());
            return TokenValidationResponse.builder()
                    .valid(false)
                    .message("토큰 검증 중 오류가 발생했습니다.")
                    .build();
        }
    }


    // 토큰 갱신

    public TokenDto refreshTokens(String refreshToken) {
        // Access Token 을 Refresh 엔드포인트로 재사용하는 것을 차단합니다.
        if (!validateRefreshToken(refreshToken)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "유효하지 않은 Refresh Token입니다.");
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
    }


    // 토큰에서 Authorization 헤더 추출

    public String extractTokenFromAuthorizationHeader(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        }
        return null;
    }
} 