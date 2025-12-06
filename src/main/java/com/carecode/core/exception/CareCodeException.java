package com.carecode.core.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * CareCode 애플리케이션의 기본 예외 클래스
 * 모든 커스텀 예외는 이 클래스를 상속받아야 함
 */
@Getter
public abstract class CareCodeException extends RuntimeException {
    
    private final ErrorCode errorCode;
    private final HttpStatus httpStatus;
    
    protected CareCodeException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.httpStatus = errorCode.getHttpStatus();
    }
    
    protected CareCodeException(ErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.errorCode = errorCode;
        this.httpStatus = errorCode.getHttpStatus();
    }
    
    protected CareCodeException(ErrorCode errorCode, String customMessage, Throwable cause) {
        super(customMessage, cause);
        this.errorCode = errorCode;
        this.httpStatus = errorCode.getHttpStatus();
    }
}

