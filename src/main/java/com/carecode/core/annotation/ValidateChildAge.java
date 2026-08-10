package com.carecode.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 자녀 연령 검증을 위한 어노테이션 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidateChildAge {
    String message = "자녀 연령이 유효하지 않습니다.";

    int minAge() default 0;
    int maxAge() default 12;
    String message() default message;
} 