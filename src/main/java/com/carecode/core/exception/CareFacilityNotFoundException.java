package com.carecode.core.exception;

/**
 * 돌봄 시설을 찾을 수 없을 때 발생하는 예외
 */
public class CareFacilityNotFoundException extends ResourceNotFoundException {
    
    public CareFacilityNotFoundException(String message) {
        super(message);
    }
    
    public CareFacilityNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public CareFacilityNotFoundException(String facilityType, String region) {
        super(String.format("지역: %s에서 %s 시설을 찾을 수 없습니다.", region, facilityType));
    }
} 