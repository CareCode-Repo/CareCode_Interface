package com.carecode.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 메서드 실행 시간을 측정하는 어노테이션
 * 육아 관련 API의 성능 모니터링에 활용
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LogExecutionTime {
    /**
     * 로그 메시지 (선택사항)
     */
    String value() default "";
    
    /**
     * 메서드 인자 로깅 여부
     */
    boolean logArgs() default false;
    
    /**
     * 경고 임계값 (밀리초). 이 값을 초과하면 WARN 레벨로 로깅
     * 0이면 경고 없음
     */
    long warnThreshold() default 0;
} 