package com.carecode.core.exception;

/**
 * Rate limit 초과 시 발생하는 예외 (HTTP 429).
 */
public class RateLimitExceededException extends CareCodeException {

    public RateLimitExceededException() {
        super(ErrorCode.RATE_LIMIT_EXCEEDED);
    }

    public RateLimitExceededException(String message) {
        super(ErrorCode.RATE_LIMIT_EXCEEDED, message);
    }
}
