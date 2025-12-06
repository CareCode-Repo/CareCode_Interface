package com.carecode.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * API Rate Limiting을 위한 어노테이션
 * 지정된 시간 내에 허용되는 최대 요청 수를 제한합니다.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    /**
     * 시간당 허용되는 최대 요청 수
     */
    int requests() default 100;
    
    /**
     * 시간 윈도우 (초 단위)
     */
    int windowSeconds() default 60;
    
    /**
     * Rate limit 초과 시 메시지
     */
    String message() default "요청 한도를 초과했습니다. 잠시 후 다시 시도해주세요.";
    
    /**
     * 사용자별로 제한할지 여부 (true면 IP 기반, false면 전역)
     */
    boolean perUser() default true;
}

