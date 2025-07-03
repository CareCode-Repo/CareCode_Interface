package com.carecode.core.exception;

/**
 * 육아 정책을 찾을 수 없을 때 발생하는 예외
 */
public class PolicyNotFoundException extends ResourceNotFoundException {
    
    public PolicyNotFoundException(String message) {
        super(message);
    }
    
    public PolicyNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public PolicyNotFoundException(String region, String policyType) {
        super(String.format("지역: %s, 정책 유형: %s에 해당하는 육아 정책을 찾을 수 없습니다.", region, policyType));
    }
} 