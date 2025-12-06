package com.carecode.core.exception;

/**
 * 건강 기록을 찾을 수 없을 때 발생하는 예외
 */
public class HealthRecordNotFoundException extends CareCodeException {
    
    public HealthRecordNotFoundException(Long recordId) {
        super(ErrorCode.HEALTH_RECORD_NOT_FOUND, 
              ErrorCode.HEALTH_RECORD_NOT_FOUND.getMessage() + ": " + recordId);
    }
    
    public HealthRecordNotFoundException(String message) {
        super(ErrorCode.HEALTH_RECORD_NOT_FOUND, message);
    }
}

