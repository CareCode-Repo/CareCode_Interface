package com.carecode.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 인증이 필요한 API를 위한 어노테이션
 * 육아 커뮤니티, 개인정보 등 민감한 기능에 활용
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireAuthentication {
    String[] roles() default {};
    String message() default "인증이 필요합니다.";
} 