package com.carecode.core.handler;

import com.carecode.core.exception.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 전역 예외 핸들러
 * 모든 예외를 일관된 형식으로 처리
 */
@Slf4j
@RestControllerAdvice
public class CustomizedResponseEntityExceptionHandler {

    /**
     * CareCodeException 계층의 예외 처리
     */
    @ExceptionHandler(CareCodeException.class)
    public ResponseEntity<ErrorResponse> handleCareCodeException(CareCodeException ex, WebRequest request) {
        log.warn("CareCodeException 발생: {} - {}", ex.getErrorCode().getCode(), ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.of(
            ex.getErrorCode(),
            ex.getMessage(),
            request.getDescription(false)
        );
        
        return ResponseEntity
                .status(ex.getHttpStatus())
                .body(errorResponse);
    }

    /**
     * UserNotFoundException 처리 (하위 호환성 유지)
     */
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFoundException(UserNotFoundException ex, WebRequest request) {
        log.warn("UserNotFoundException 발생: {}", ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.of(
            ErrorCode.USER_NOT_FOUND,
            ex.getMessage(),
            request.getDescription(false)
        );
        
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(errorResponse);
    }

    /**
     * ResourceNotFoundException 처리 (하위 호환성 유지)
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException ex, WebRequest request) {
        log.warn("ResourceNotFoundException 발생: {}", ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.of(
            ErrorCode.RESOURCE_NOT_FOUND,
            ex.getMessage(),
            request.getDescription(false)
        );
        
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(errorResponse);
    }

    /**
     * BusinessException 처리 (하위 호환성 유지)
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex, WebRequest request) {
        log.warn("BusinessException 발생: {}", ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.of(
            ErrorCode.INVALID_INPUT,
            ex.getMessage(),
            request.getDescription(false)
        );
        
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorResponse);
    }

    /**
     * CareServiceException 처리 (하위 호환성 유지)
     */
    @ExceptionHandler(CareServiceException.class)
    public ResponseEntity<ErrorResponse> handleCareServiceException(CareServiceException ex, WebRequest request) {
        log.error("CareServiceException 발생: {} - {}", ex.getErrorCode(), ex.getMessage(), ex);
        
        ErrorCode errorCode = ex.getErrorCode() != null 
            ? ErrorCode.INTERNAL_SERVER_ERROR 
            : ErrorCode.INTERNAL_SERVER_ERROR;
        
        ErrorResponse errorResponse = ErrorResponse.of(
            errorCode,
            ex.getMessage(),
            request.getDescription(false)
        );
        
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse);
    }

    /**
     * PolicyNotFoundException 처리 (하위 호환성 유지)
     */
    @ExceptionHandler(PolicyNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePolicyNotFoundException(PolicyNotFoundException ex, WebRequest request) {
        log.warn("PolicyNotFoundException 발생: {}", ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.of(
            ErrorCode.POLICY_NOT_FOUND,
            ex.getMessage(),
            request.getDescription(false)
        );
        
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(errorResponse);
    }

    /**
     * CareFacilityNotFoundException 처리 (하위 호환성 유지)
     */
    @ExceptionHandler(CareFacilityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCareFacilityNotFoundException(CareFacilityNotFoundException ex, WebRequest request) {
        log.warn("CareFacilityNotFoundException 발생: {}", ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.of(
            ErrorCode.CARE_FACILITY_NOT_FOUND,
            ex.getMessage(),
            request.getDescription(false)
        );
        
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(errorResponse);
    }

    /**
     * Validation 예외 처리 (@Valid 실패)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex, WebRequest request) {
        log.warn("Validation 실패: {}", ex.getMessage());
        
        Map<String, String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                    FieldError::getField,
                    error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : "유효하지 않은 값입니다",
                    (existing, replacement) -> existing
                ));
        
        String details = errors.entrySet().stream()
                .map(e -> e.getKey() + ": " + e.getValue())
                .collect(Collectors.joining(", "));
        
        ErrorResponse errorResponse = ErrorResponse.of(
            ErrorCode.INVALID_INPUT,
            "입력값 검증에 실패했습니다",
            details
        );
        
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorResponse);
    }

    /**
     * IllegalArgumentException 처리
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
        log.warn("IllegalArgumentException 발생: {}", ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.of(
            ErrorCode.INVALID_INPUT,
            ex.getMessage(),
            request.getDescription(false)
        );
        
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorResponse);
    }

    /**
     * 모든 예외 처리 (최후의 수단)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAllExceptions(Exception ex, WebRequest request) {
        log.error("예상치 못한 예외 발생", ex);
        
        ErrorResponse errorResponse = ErrorResponse.of(
            ErrorCode.INTERNAL_SERVER_ERROR,
            "서버 내부 오류가 발생했습니다",
            request.getDescription(false)
        );
        
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse);
    }
}