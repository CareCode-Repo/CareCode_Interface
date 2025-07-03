package com.carecode.core.exception;

/**
 * 육아 서비스 전용 예외 클래스
 * 육아 관련 비즈니스 로직에서 발생하는 예외를 처리
 */
public class CareServiceException extends RuntimeException {
    
    private final String errorCode;
    
    public CareServiceException(String message) {
        super(message);
        this.errorCode = "CARE_SERVICE_ERROR";
    }
    
    public CareServiceException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "CARE_SERVICE_ERROR";
    }
    
    public CareServiceException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public CareServiceException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
} 