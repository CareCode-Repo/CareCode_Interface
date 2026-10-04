package com.carecode.domain.admin.dto.response;

import com.carecode.external.geocoding.FacilityGeocodingService.GeocodingResult;

/**
 * 좌표 보정 실행 결과.
 *
 * <p>{@code skippedReason} 이 있으면 아예 실행하지 않은 것이다(키 미설정 등). 그때 보정 0 건을
 * "보정할 게 없었다" 로 읽으면 키가 빠진 것을 모른 채 지나간다.
 */
public record GeocodingResultResponse(
        int resolved,
        int failed,
        long remaining,
        String skippedReason) {

    public static GeocodingResultResponse from(GeocodingResult result) {
        return new GeocodingResultResponse(
                result.getResolved(), result.getFailed(), result.getRemaining(), result.getSkippedReason());
    }
}
