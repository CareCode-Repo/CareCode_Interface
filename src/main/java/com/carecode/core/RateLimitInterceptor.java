package com.carecode.core;

import com.carecode.core.util.ClientIpResolver;
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
import java.util.List;

/**
 * 전 구간 기본 rate limit.
 *
 * <ul>
 *   <li>인증된 사용자: userId 기반 분당 300회 (NAT/공유 IP 환경 대응)</li>
 *   <li>미인증 요청: IP 기반 분당 120회</li>
 *   <li>민감 엔드포인트(로그인·가입·인증코드): IP 기반 분당 30회</li>
 * </ul>
 *
 * <p>여기는 어디까지나 하한선이다. 호출 한 건이 비용이 되는 API(챗봇의 LLM 호출)나
 * 무차별 대입 대상(로그인, 인증코드 검증)은 이 값으로 부족하므로
 * {@code @RateLimit} 으로 엔드포인트별 상한을 따로 건다.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate redisTemplate;
    private final ClientIpResolver clientIpResolver;

    private static final String RATE_LIMIT_KEY_PREFIX = "ratelimit:";
    private static final int AUTHENTICATED_LIMIT    = 300;  // 인증 사용자 (userId 기준)
    private static final int ANONYMOUS_LIMIT        = 120;  // 미인증 (IP 기준)
    private static final int PUBLIC_SENSITIVE_LIMIT = 30;   // 로그인·가입 등 민감 엔드포인트
    private static final Duration WINDOW_DURATION = Duration.ofMinutes(1);

    /**
     * 낮은 한도를 적용할 공개 엔드포인트.
     *
     * <p>예전에는 {@code /api/v1/contact}, {@code /api/v1/auth/signup} 을 보고 있었다.
     * 이 애플리케이션에는 {@code /api/v1} 로 매핑된 컨트롤러가 하나도 없어서
     * (BaseController 의 {@code @RequestMapping("/api/v1")} 은 하위 클래스가 전부 덮어쓴다)
     * 민감 엔드포인트 등급이 한 번도 적용된 적이 없었다.
     */
    private static final List<String> PUBLIC_SENSITIVE_PREFIXES = List.of(
            "/auth/login",
            "/auth/register",
            "/auth/refresh",
            "/auth/send-code",
            "/auth/verify-code",
            "/auth/kakao"
    );

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

        Long currentCount = incrementQuietly(key);
        if (currentCount == null) {
            // 카운터를 못 읽었다. 제한을 걸 근거가 없으므로 통과시킨다(fail-open).
            return true;
        }

        long count = currentCount;
        if (count > limit) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"success\":false,\"message\":\"요청이 너무 많습니다. 잠시 후 다시 시도해주세요.\",\"errorCode\":\"RATE_LIMIT_EXCEEDED\"}");
            return false;
        }

        writeRateLimitHeaders(response, key, limit, count);
        return true;
    }

    /**
     * 카운터를 증가시키고 현재 값을 돌려준다. 실패하면 null.
     *
     * <p>Redis 장애가 전체 API 중단으로 번지면 안 된다. 예전에는 {@code DataAccessException}
     * 만 잡았는데, 그 바깥에서 나는 실패(연결 팩토리가 없어 {@code opsForValue()} 가 null 이거나
     * 직렬화 단계에서 나는 오류 등)는 그대로 500 이 됐다. 여기서는 어떤 런타임 실패든 통과시킨다.
     */
    private Long incrementQuietly(String key) {
        try {
            Long currentCount = redisTemplate.opsForValue().increment(key);
            if (currentCount != null && currentCount == 1L) {
                redisTemplate.expire(key, WINDOW_DURATION);
            }
            return currentCount;
        } catch (RuntimeException e) {
            log.error("Rate limit 확인 실패 - 제한을 건너뜁니다. key={}", key, e);
            return null;
        }
    }

    /** 남은 한도 안내 헤더. 이 조회가 실패해도 요청 자체는 막지 않는다. */
    private void writeRateLimitHeaders(HttpServletResponse response, String key, int limit, long count) {
        long ttlSeconds = 60;
        try {
            Long ttl = redisTemplate.getExpire(key);
            if (ttl != null && ttl > 0) {
                ttlSeconds = ttl;
            }
        } catch (RuntimeException e) {
            log.debug("Rate limit TTL 조회 실패 - 기본값으로 헤더를 채웁니다. key={}", key);
        }

        response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, limit - count)));
        response.setHeader("X-RateLimit-Reset", String.valueOf((System.currentTimeMillis() / 1000) + ttlSeconds));
    }

    /** SecurityContext에서 인증된 사용자 ID 추출. 미인증이면 null. */
    private String resolveUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        return auth.getName();
    }

    /** 클라이언트 IP 추출. 프록시 헤더 신뢰 여부는 ClientIpResolver 가 설정에 따라 판단한다. */
    private String getClientIp(HttpServletRequest request) {
        return clientIpResolver.resolve(request);
    }

    /** 공개 API 중 민감한 엔드포인트 (낮은 rate limit 적용) */
    private boolean isPublicSensitiveEndpoint(String path) {
        return PUBLIC_SENSITIVE_PREFIXES.stream().anyMatch(path::startsWith);
    }
}
