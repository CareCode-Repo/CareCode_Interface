package com.carecode.core.aspect;

import com.carecode.core.annotation.CacheableResult;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Aspect
@Component
@Slf4j
public class CachingAspect {

    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

    @Around("@annotation(cacheableResult)")
    public Object cacheResult(ProceedingJoinPoint joinPoint, CacheableResult cacheableResult) throws Throwable {
        String cacheKey = generateCacheKey(joinPoint, cacheableResult);
        
        // 캐시에서 결과 확인
        CacheEntry cachedEntry = cache.get(cacheKey);
        if (cachedEntry != null && !cachedEntry.isExpired()) {
            log.debug("캐시에서 결과 반환: {}", cacheKey);
            return cachedEntry.getValue();
        }
        
        // 메서드 실행
        Object result = joinPoint.proceed();
        
        // 결과 캐싱
        long ttl = cacheableResult.ttl() * 1000; // 초를 밀리초로 변환
        cache.put(cacheKey, new CacheEntry(result, System.currentTimeMillis() + ttl));
        
        log.debug("결과를 캐시에 저장: {}", cacheKey);
        return result;
    }
    
    private String generateCacheKey(ProceedingJoinPoint joinPoint, CacheableResult cacheableResult) {
        if (!cacheableResult.key().isEmpty()) {
            return cacheableResult.key();
        }
        
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        return className + "." + methodName;
    }
    
    private static class CacheEntry {
        private final Object value;
        private final long expirationTime;
        
        public CacheEntry(Object value, long expirationTime) {
            this.value = value;
            this.expirationTime = expirationTime;
        }
        
        public Object getValue() {
            return value;
        }
        
        public boolean isExpired() {
            return System.currentTimeMillis() > expirationTime;
        }
    }
} 