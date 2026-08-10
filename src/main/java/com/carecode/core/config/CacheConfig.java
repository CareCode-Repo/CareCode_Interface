package com.carecode.core.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
 * Redis 캐시 설정.
 * spring.cache.type=none 이면 이 설정을 만들지 않는다 — 그렇지 않으면 Redis 없이 로컬·테스트 구동이 불가능하다.
 */
@Configuration
@EnableCaching
@ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis", matchIfMissing = true)
public class CacheConfig {

    // 기본 캐시 설정
    private RedisCacheConfiguration defaultCacheConfig() {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10)) // 기본 TTL: 10분
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer(cacheObjectMapper())))
                .disableCachingNullValues(); // null 값은 캐싱하지 않음
    }

    /** 캐시 값 직렬화용 ObjectMapper. JavaTimeModule 이 없으면 LocalDate/LocalDateTime 필드를 가진 DTO 캐싱이 실패한다. */
    private ObjectMapper cacheObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // 역직렬화 시 구체 타입을 복원하기 위한 타입 정보 포함
        mapper.activateDefaultTyping(
                BasicPolymorphicTypeValidator.builder()
                        .allowIfSubType("com.carecode.")
                        .allowIfSubType("java.util.")
                        .allowIfSubType("java.time.")
                        .build(),
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY);
        return mapper;
    }

    // 캐시별 TTL 설정
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

