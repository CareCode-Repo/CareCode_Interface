package com.carecode.core.aspect;

import com.carecode.core.annotation.CacheableResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.concurrent.TimeUnit;

/**
 * Redis를 사용한 캐싱 Aspect
 * @CacheableResult 어노테이션이 붙은 메서드의 결과를 Redis에 캐싱합니다.
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class CachingAspect {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Around("@annotation(cacheableResult)")
    public Object cacheResult(ProceedingJoinPoint joinPoint, CacheableResult cacheableResult) throws Throwable {
        String cacheKey = generateCacheKey(joinPoint, cacheableResult);
        
        try {
            // Redis에서 캐시 확인
            String cachedValue = redisTemplate.opsForValue().get(cacheKey);
            if (cachedValue != null) {
                log.debug("캐시 히트 - Key: {}", cacheKey);
                try {
                    Signature signature = joinPoint.getSignature();
                    if (signature instanceof MethodSignature) {
                        MethodSignature methodSignature = (MethodSignature) signature;
                        Method method = methodSignature.getMethod();
                        Type returnType = method.getGenericReturnType();
                        return deserialize(cachedValue, returnType);
                    }
                } catch (Exception e) {
                    log.warn("캐시 역직렬화 실패 - Key: {}, 오류: {}", cacheKey, e.getMessage());
                    // 역직렬화 실패 시 캐시 삭제하고 메서드 실행
                    redisTemplate.delete(cacheKey);
                }
            }
            
            // 메서드 실행
            Object result = joinPoint.proceed();
            
            // Redis에 캐싱
            String serializedValue = serialize(result);
            redisTemplate.opsForValue().set(
                cacheKey, 
                serializedValue, 
                cacheableResult.ttl(), 
                TimeUnit.SECONDS
            );
            
            log.debug("캐시 저장 - Key: {}, TTL: {}초", cacheKey, cacheableResult.ttl());
            return result;
            
        } catch (Exception e) {
            log.error("캐싱 처리 중 오류 발생 - Key: {}", cacheKey, e);
            // 캐싱 실패 시에도 메서드는 정상 실행
            return joinPoint.proceed();
        }
    }
    
    private String generateCacheKey(ProceedingJoinPoint joinPoint, CacheableResult cacheableResult) {
        if (!cacheableResult.key().isEmpty()) {
            return cacheableResult.cacheName() + ":" + cacheableResult.key();
        }
        
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        Object[] args = joinPoint.getArgs();
        
        StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append(cacheableResult.cacheName())
                  .append(":")
                  .append(className)
                  .append(".")
                  .append(methodName);
        
        if (args != null && args.length > 0) {
            keyBuilder.append(":");
            for (int i = 0; i < args.length; i++) {
                if (i > 0) keyBuilder.append(",");
                keyBuilder.append(args[i] != null ? args[i].toString() : "null");
            }
        }
        
        return keyBuilder.toString();
    }
    
    private String serialize(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }
    
    @SuppressWarnings("unchecked")
    private <T> T deserialize(String json, Type returnType) throws Exception {
        if (returnType instanceof Class) {
            return (T) objectMapper.readValue(json, (Class<?>) returnType);
        } else if (returnType instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) returnType;
            TypeFactory typeFactory = objectMapper.getTypeFactory();
            return (T) objectMapper.readValue(json, typeFactory.constructType(returnType));
        } else {
            // 기본적으로 Object로 역직렬화
            return (T) objectMapper.readValue(json, Object.class);
        }
    }
} 