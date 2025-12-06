package com.carecode.core.exception;

/**
 * 병원을 찾을 수 없을 때 발생하는 예외
 */
public class HospitalNotFoundException extends CareCodeException {
    
    public HospitalNotFoundException(Long hospitalId) {
        super(ErrorCode.HOSPITAL_NOT_FOUND, 
              ErrorCode.HOSPITAL_NOT_FOUND.getMessage() + ": " + hospitalId);
    }
    
    public HospitalNotFoundException(String message) {
        super(ErrorCode.HOSPITAL_NOT_FOUND, message);
    }
}

