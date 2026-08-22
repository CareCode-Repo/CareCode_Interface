package com.carecode.core.aspect;

import com.carecode.core.annotation.RateLimit;
import com.carecode.core.exception.RateLimitExceededException;
import com.carecode.core.util.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;

/** RateLimit 이 붙은 메서드에 대한 요청 수 제한. 이전 구현은 인스턴스 로컬 ConcurrentHashMap 을 썼기 때문에 (1) 다중 인스턴스에서 무의미했고 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class RateLimitingAspect {

    private static final String KEY_PREFIX = "ratelimit:method:";

    private final StringRedisTemplate redisTemplate;
    private final ClientIpResolver clientIpResolver;

    @Around("@annotation(rateLimit)")
    public Object rateLimit(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        String key = KEY_PREFIX + generateKey(joinPoint, rateLimit);

        long count;
        try {
            Long incremented = redisTemplate.opsForValue().increment(key);
            count = incremented != null ? incremented : 0L;
            if (count == 1L) {
                redisTemplate.expire(key, Duration.ofSeconds(rateLimit.windowSeconds()));
            }
        } catch (DataAccessException e) {
            // Redis 장애로 전체 API 가 막히지 않도록 fail-open 한다.
            log.error("Rate limit 카운터 조회 실패 - Redis 장애로 제한을 건너뜁니다. key={}", key, e);
            return joinPoint.proceed();
        }

        if (count > rateLimit.requests()) {
            log.warn("Rate limit 초과 - key={}, 요청 수={}, 한도={}", key, count, rateLimit.requests());
            throw new RateLimitExceededException(rateLimit.message());
        }

        return joinPoint.proceed();
    }

    /**
     * 호출자별 카운터 키를 만든다.
     *
     * <p>로그인한 요청은 IP 가 아니라 계정으로 센다. IP 로만 세면 (1) 같은 회사·학교·통신사 NAT
     * 뒤의 사용자들이 한도를 나눠 쓰게 되고, (2) 챗봇처럼 계정 단위로 비용이 나가는 API 에서
     * 한 사람이 IP 만 바꿔가며 한도를 초과할 수 있다.
     *
     * <p>비로그인 요청(로그인·회원가입·인증코드 발송)은 계정이 없으므로 IP 로 센다.
     */
    private String generateKey(ProceedingJoinPoint joinPoint, RateLimit rateLimit) {
        String methodName = joinPoint.getSignature().toShortString();

        if (!rateLimit.perUser()) {
            return methodName;
        }

        String principal = currentPrincipal();
        if (principal != null) {
            return methodName + ":user:" + principal;
        }

        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return methodName;
        }

        HttpServletRequest request = attributes.getRequest();
        return methodName + ":ip:" + clientIpResolver.resolve(request);
    }

    /** 인증된 호출자의 식별자. 비인증이면 null. */
    private String currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        String name = authentication.getName();
        return (name == null || name.isBlank() || "anonymousUser".equals(name)) ? null : name;
    }
}
