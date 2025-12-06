package com.carecode.core.exception;

/**
 * 아동을 찾을 수 없을 때 발생하는 예외
 */
public class ChildNotFoundException extends CareCodeException {
    
    public ChildNotFoundException(Long childId) {
        super(ErrorCode.CHILD_NOT_FOUND, 
              ErrorCode.CHILD_NOT_FOUND.getMessage() + ": " + childId);
    }
    
    public ChildNotFoundException(String message) {
        super(ErrorCode.CHILD_NOT_FOUND, message);
    }
}

