package com.carecode.core.aspect;

import com.carecode.core.annotation.RateLimit;
import com.carecode.core.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rate Limiting을 처리하는 Aspect
 * 지정된 시간 내에 허용되는 최대 요청 수를 제한합니다.
 */
@Aspect
@Component
@Slf4j
public class RateLimitingAspect {

    private final ConcurrentHashMap<String, RateLimitInfo> rateLimitMap = new ConcurrentHashMap<>();

    @Around("@annotation(rateLimit)")
    public Object rateLimit(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        String key = generateKey(joinPoint, rateLimit);
        RateLimitInfo info = rateLimitMap.computeIfAbsent(key, k -> new RateLimitInfo(rateLimit));
        
        long currentTime = System.currentTimeMillis();
        
        // 시간 윈도우가 지났으면 리셋
        if (currentTime - info.getWindowStart() > rateLimit.windowSeconds() * 1000L) {
            info.reset(currentTime);
        }
        
        // 요청 수 체크
        int currentCount = info.getCount().incrementAndGet();
        
        if (currentCount > rateLimit.requests()) {
            log.warn("Rate limit 초과 - Key: {}, 현재 요청 수: {}, 허용 한도: {}", 
                key, currentCount, rateLimit.requests());
            throw new BusinessException(rateLimit.message());
        }
        
        return joinPoint.proceed();
    }
    
    private String generateKey(ProceedingJoinPoint joinPoint, RateLimit rateLimit) {
        String methodName = joinPoint.getSignature().toShortString();
        
        if (rateLimit.perUser()) {
            // IP 기반 키 생성
            ServletRequestAttributes attributes = 
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String clientIp = getClientIp(request);
                return methodName + ":" + clientIp;
            }
        }
        
        return methodName;
    }
    
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
    
    private static class RateLimitInfo {
        private final RateLimit rateLimit;
        private long windowStart;
        private final AtomicInteger count;
        
        public RateLimitInfo(RateLimit rateLimit) {
            this.rateLimit = rateLimit;
            this.windowStart = System.currentTimeMillis();
            this.count = new AtomicInteger(0);
        }
        
        public void reset(long currentTime) {
            this.windowStart = currentTime;
            this.count.set(0);
        }
        
        public long getWindowStart() {
            return windowStart;
        }
        
        public AtomicInteger getCount() {
            return count;
        }
    }
}

