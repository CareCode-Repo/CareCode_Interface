package com.carecode.domain.health.dto.response;

import java.util.List;

/**
 * 아이 연령 기준 연계 추천.
 *
 * <p>추천 정책·시설은 **이름 문자열만** 담는다. id 가 없어 상세로 바로 보낼 수 없고, 화면에서는
 * 검색 지름길로 쓴다. (id 를 주려면 정책·시설 쪽 추천 API 를 쓰는 게 맞다.)
 *
 * <p>등록된 자녀가 없으면 {@code childAge} 가 0 이다. 그 경우 추천은 0세 기준이 되므로,
 * 화면은 "자녀를 등록하세요" 를 먼저 보여 주는 쪽이 정확하다.
 */
public record HealthRecommendationResponse(
        String userId,
        int childAge,
        List<String> recommendedPolicies,
        List<String> recommendedFacilities,
        String nudgeMessage) {
}
