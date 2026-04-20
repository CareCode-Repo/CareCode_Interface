package com.carecode.core;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;

/**
 * Rate Limiting 인터셉터
 *
 * - 인증된 사용자: userId 기반 분당 300회 (NAT/공유 IP 환경 대응)
 * - 미인증 요청:  IP 기반 분당 120회
 * - 민감 공개 API(/auth/signup 등): IP 기반 분당 30회
 *
 * 학교 환경처럼 다수 사용자가 동일 공인 IP를 쓰는 경우
 * IP 기반 단일 제한은 오탐이 많아 인증 여부로 키를 분리합니다.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate redisTemplate;

    private static final String RATE_LIMIT_KEY_PREFIX = "ratelimit:";
    private static final int AUTHENTICATED_LIMIT    = 300;    // 인증 사용자 (userId 기준)
    private static final int ANONYMOUS_LIMIT        = 120;    // 미인증 (IP 기준)
    private static final int PUBLIC_SENSITIVE_LIMIT = 30;   // 회원가입 등 민감 엔드포인트
    private static final Duration WINDOW_DURATION = Duration.ofMinutes(1);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();

        // 민감 공개 엔드포인트는 IP 기반 낮은 제한 유지
        if (isPublicSensitiveEndpoint(path)) {
            return checkLimit("ip:" + getClientIp(request), PUBLIC_SENSITIVE_LIMIT, response);
        }

        // 인증된 사용자 → userId 기반 (NAT 문제 없음)
        String userId = resolveUserId();
        if (userId != null) {
            return checkLimit("user:" + userId, AUTHENTICATED_LIMIT, response);
        }

        // 미인증 → IP 기반
        return checkLimit("ip:" + getClientIp(request), ANONYMOUS_LIMIT, response);
    }

    private boolean checkLimit(String keyBody, int limit, HttpServletResponse response) throws Exception {
        String key = RATE_LIMIT_KEY_PREFIX + keyBody;
        Long currentCount = redisTemplate.opsForValue().increment(key);

        if (currentCount != null && currentCount == 1) {
            redisTemplate.expire(key, WINDOW_DURATION);
        }

        long count = currentCount != null ? currentCount : 0;

        if (count > limit) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"success\":false,\"message\":\"요청이 너무 많습니다. 잠시 후 다시 시도해주세요.\",\"errorCode\":\"RATE_LIMIT_EXCEEDED\"}");
            return false;
        }

        Long ttl = redisTemplate.getExpire(key);
        long resetTime = (System.currentTimeMillis() / 1000) + (ttl != null && ttl > 0 ? ttl : 60);
        response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, limit - count)));
        response.setHeader("X-RateLimit-Reset", String.valueOf(resetTime));
        return true;
    }

    /**
     * SecurityContext에서 인증된 사용자 ID 추출. 미인증이면 null.
     */
    private String resolveUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        return auth.getName();
    }

    /**
     * 클라이언트 IP 추출 (프록시 고려)
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /**
     * 공개 API 중 민감한 엔드포인트 (낮은 rate limit 적용)
     */
    private boolean isPublicSensitiveEndpoint(String path) {
        return path.startsWith("/api/v1/contact") ||
                path.startsWith("/api/v1/auth/signup");
    }
}

