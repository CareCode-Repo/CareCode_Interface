package com.carecode.core.util;

import com.carecode.core.exception.BusinessException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * 공통 검증 유틸리티 클래스
 * DTO 검증을 일관되게 처리합니다.
 */
@Component
@RequiredArgsConstructor
public class ValidationUtil {

    private final Validator validator;

    /**
     * 객체를 검증하고 위반 사항이 있으면 예외를 발생시킵니다.
     * 
     * @param object 검증할 객체
     * @param <T> 객체 타입
     * @throws BusinessException 검증 실패 시
     */
    public <T> void validate(T object) {
        Set<ConstraintViolation<T>> violations = validator.validate(object);
        
        if (!violations.isEmpty()) {
            String errorMessage = violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .collect(Collectors.joining(", "));
            throw new BusinessException("검증 실패: " + errorMessage);
        }
    }

    /**
     * 객체를 검증하고 위반 사항이 있으면 예외를 발생시킵니다.
     * 커스텀 메시지를 포함합니다.
     * 
     * @param object 검증할 객체
     * @param message 커스텀 에러 메시지
     * @param <T> 객체 타입
     * @throws BusinessException 검증 실패 시
     */
    public <T> void validate(T object, String message) {
        Set<ConstraintViolation<T>> violations = validator.validate(object);
        
        if (!violations.isEmpty()) {
            String errorMessage = violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .collect(Collectors.joining(", "));
            throw new BusinessException(message + ": " + errorMessage);
        }
    }

    /**
     * 특정 그룹으로 객체를 검증합니다.
     * 
     * @param object 검증할 객체
     * @param groups 검증 그룹
     * @param <T> 객체 타입
     * @throws BusinessException 검증 실패 시
     */
    public <T> void validate(T object, Class<?>... groups) {
        Set<ConstraintViolation<T>> violations = validator.validate(object, groups);
        
        if (!violations.isEmpty()) {
            String errorMessage = violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .collect(Collectors.joining(", "));
            throw new BusinessException("검증 실패: " + errorMessage);
        }
    }

    /**
     * 검증 결과를 반환합니다 (예외 발생 없이).
     * 
     * @param object 검증할 객체
     * @param <T> 객체 타입
     * @return 검증 실패 메시지 목록 (비어있으면 검증 통과)
     */
    public <T> Set<String> validateAndGetErrors(T object) {
        Set<ConstraintViolation<T>> violations = validator.validate(object);
        return violations.stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());
    }
}

