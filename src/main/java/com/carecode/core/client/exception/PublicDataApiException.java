package com.carecode.core.client.exception;

/**
 * 공공데이터 API 관련 예외
 * API 호출 실패 시 발생하는 커스텀 예외
 */
public class PublicDataApiException extends RuntimeException {

    public PublicDataApiException(String message) {
        super(message);
    }

    public PublicDataApiException(String message, Throwable cause) {
        super(message, cause);
    }

    public PublicDataApiException(Throwable cause) {
        super(cause);
    }
} 