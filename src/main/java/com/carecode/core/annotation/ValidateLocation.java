package com.carecode.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 위치 기반 검증을 위한 어노테이션
 * 육아 서비스에서 지역별 정책 및 시설 검색 시 활용
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidateLocation {
    String message = "위치 정보가 필요합니다.";

    boolean required() default true;
    String message() default message;
} 