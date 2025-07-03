package com.carecode.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 결과 캐싱을 위한 어노테이션
 * 육아 정책 정보, 시설 정보 등 자주 조회되는 데이터의 성능 향상에 활용
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CacheableResult {
    String cacheName() default "";
    long ttl() default 3600; // 기본 1시간
    String key() default "";
} 