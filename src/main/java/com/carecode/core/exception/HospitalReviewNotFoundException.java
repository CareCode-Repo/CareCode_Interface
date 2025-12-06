package com.carecode.core.exception;

/**
 * 병원 리뷰를 찾을 수 없을 때 발생하는 예외
 */
public class HospitalReviewNotFoundException extends CareCodeException {
    
    public HospitalReviewNotFoundException(Long reviewId) {
        super(ErrorCode.HOSPITAL_REVIEW_NOT_FOUND, 
              ErrorCode.HOSPITAL_REVIEW_NOT_FOUND.getMessage() + ": " + reviewId);
    }
    
    public HospitalReviewNotFoundException(String message) {
        super(ErrorCode.HOSPITAL_REVIEW_NOT_FOUND, message);
    }
}

