package com.carecode.core.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis 캐시 설정
 * Spring Cache Abstraction을 사용한 캐싱 전략
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * 기본 캐시 설정
     */
    private RedisCacheConfiguration defaultCacheConfig() {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10)) // 기본 TTL: 10분
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues(); // null 값은 캐싱하지 않음
    }

    /**
     * 캐시별 TTL 설정
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory factory) {
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        // 건강 기록 캐시: 5분
        cacheConfigurations.put("healthRecords", defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5)));
        
        // 정책 캐시: 30분 (변경 빈도가 낮음)
        cacheConfigurations.put("policy", defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30)));
        
        // 돌봄 시설 캐시: 15분
        cacheConfigurations.put("careFacility", defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(15)));
        
        // 사용자 정보 캐시: 10분
        cacheConfigurations.put("user", defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10)));
        
        // 통계 데이터 캐시: 1분 (자주 변경됨)
        cacheConfigurations.put("statistics", defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(1)));
        
        return RedisCacheManager.builder(factory)
                .cacheDefaults(defaultCacheConfig())
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware() // 트랜잭션 인식
                .build();
    }
}

