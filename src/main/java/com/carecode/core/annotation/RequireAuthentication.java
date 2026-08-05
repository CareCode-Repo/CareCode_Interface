package com.carecode.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 인증이 필요한 API를 위한 어노테이션 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireAuthentication {
    String message = "인증이 필요합니다.";

    String[] roles() default {};
    String message() default message;
} 