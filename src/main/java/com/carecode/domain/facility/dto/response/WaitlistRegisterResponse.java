package com.carecode.domain.facility.dto.response;

/**
 * 대기 등록 결과.
 *
 * <p>등록 직후 프런트가 "방금 등록한 기록" 을 가리킬 수 있어야 하므로 id 를 돌려준다.
 */
public record WaitlistRegisterResponse(Long waitlistId) {
}
