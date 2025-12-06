package com.carecode.core.exception;

/**
 * 육아 정책을 찾을 수 없을 때 발생하는 예외
 * 하위 호환성을 위해 유지
 */
public class PolicyNotFoundException extends CareCodeException {
    
    public PolicyNotFoundException(Long policyId) {
        super(ErrorCode.POLICY_NOT_FOUND, 
              ErrorCode.POLICY_NOT_FOUND.getMessage() + ": " + policyId);
    }
    
    public PolicyNotFoundException(String message) {
        super(ErrorCode.POLICY_NOT_FOUND, message);
    }
    
    public PolicyNotFoundException(String message, Throwable cause) {
        super(ErrorCode.POLICY_NOT_FOUND, message, cause);
    }
    
    public PolicyNotFoundException(String region, String policyType) {
        super(ErrorCode.POLICY_NOT_FOUND, 
              String.format("지역: %s, 정책 유형: %s에 해당하는 육아 정책을 찾을 수 없습니다.", region, policyType));
    }
} 