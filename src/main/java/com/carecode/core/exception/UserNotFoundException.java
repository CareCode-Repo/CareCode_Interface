package com.carecode.core.exception;

/**
 * 사용자를 찾을 수 없을 때 발생하는 예외
 * 하위 호환성을 위해 유지
 */
public class UserNotFoundException extends CareCodeException {
    
    public UserNotFoundException(String userId) {
        super(ErrorCode.USER_NOT_FOUND, 
              ErrorCode.USER_NOT_FOUND.getMessage() + ": " + userId);
    }
    
    public UserNotFoundException(String message, Throwable cause) {
        super(ErrorCode.USER_NOT_FOUND, message, cause);
    }
} 