package com.carecode.core.exception;

/**
 * 리소스를 찾을 수 없을 때 발생하는 예외
 * 하위 호환성을 위해 유지
 */
public class ResourceNotFoundException extends CareCodeException {
    
    public ResourceNotFoundException(String resourceName) {
        super(ErrorCode.RESOURCE_NOT_FOUND, 
              ErrorCode.RESOURCE_NOT_FOUND.getMessage() + ": " + resourceName);
    }
    
    public ResourceNotFoundException(String message, Throwable cause) {
        super(ErrorCode.RESOURCE_NOT_FOUND, message, cause);
    }
    
    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(ErrorCode.RESOURCE_NOT_FOUND, 
              String.format("%s not found with %s : '%s'", resourceName, fieldName, fieldValue));
    }
} 