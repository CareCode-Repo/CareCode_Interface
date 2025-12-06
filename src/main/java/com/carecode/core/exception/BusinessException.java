package com.carecode.core.exception;

/**
 * 비즈니스 로직 위반 시 발생하는 예외
 * 하위 호환성을 위해 유지
 */
public class BusinessException extends CareCodeException {
    
    public BusinessException(String message) {
        super(ErrorCode.INVALID_INPUT, message);
    }
    
    public BusinessException(String message, Throwable cause) {
        super(ErrorCode.INVALID_INPUT, message, cause);
    }
    
    public BusinessException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
} 