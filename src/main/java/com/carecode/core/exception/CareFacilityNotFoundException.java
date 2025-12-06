package com.carecode.core.exception;

/**
 * 돌봄 시설을 찾을 수 없을 때 발생하는 예외
 * 하위 호환성을 위해 유지
 */
public class CareFacilityNotFoundException extends CareCodeException {
    
    public CareFacilityNotFoundException(Long facilityId) {
        super(ErrorCode.CARE_FACILITY_NOT_FOUND, 
              ErrorCode.CARE_FACILITY_NOT_FOUND.getMessage() + ": " + facilityId);
    }
    
    public CareFacilityNotFoundException(String message) {
        super(ErrorCode.CARE_FACILITY_NOT_FOUND, message);
    }
    
    public CareFacilityNotFoundException(String message, Throwable cause) {
        super(ErrorCode.CARE_FACILITY_NOT_FOUND, message, cause);
    }
    
    public CareFacilityNotFoundException(String facilityType, String region) {
        super(ErrorCode.CARE_FACILITY_NOT_FOUND, 
              String.format("지역: %s에서 %s 시설을 찾을 수 없습니다.", region, facilityType));
    }
} 