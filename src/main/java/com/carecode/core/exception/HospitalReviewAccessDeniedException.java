package com.carecode.core.exception;

/**
 * 병원 리뷰에 대한 접근 권한이 없을 때 발생하는 예외
 */
public class HospitalReviewAccessDeniedException extends CareCodeException {
    
    public HospitalReviewAccessDeniedException() {
        super(ErrorCode.HOSPITAL_REVIEW_ACCESS_DENIED);
    }
    
    public HospitalReviewAccessDeniedException(String message) {
        super(ErrorCode.HOSPITAL_REVIEW_ACCESS_DENIED, message);
    }
}

