package com.carecode.domain.health.dto.response;

import java.util.List;

/**
 * 아이 연령 기준 연계 추천.
 *
 * <p>추천 정책·시설은 **이름 문자열만** 담는다. id 가 없어 상세로 바로 보낼 수 없고, 화면에서는
 * 검색 지름길로 쓴다. (id 를 주려면 정책·시설 쪽 추천 API 를 쓰는 게 맞다.)
 *
 * <p>{@code childAge} 는 <b>개월</b>이다. 정책의 대상 연령이 개월 단위라서다(부모급여 0~11,
 * 아동수당 0~95). 등록된 자녀가 없거나 생년월일이 없으면 {@code null} 이고, 그때는 추천 목록도
 * 비어 있다 — 모르는 나이를 0 으로 바꾸면 신생아 정책이 추천된다.
 *
 * <p><b>시설 추천의 한계</b>: 시설의 {@code AGE_RANGE_MIN/MAX} 는 어떤 동기화도 채우지 않아
 * 대부분 비어 있다. 연령 조건이 비면 전부 일치로 보므로, 지금 {@code recommendedFacilities} 는
 * 사실상 "운영 중인 시설 세 곳" 이다. 연령 기반이 되려면 그 값을 채우는 일이 먼저다.
 */
public record HealthRecommendationResponse(
        String userId,
        Integer childAge,
        List<String> recommendedPolicies,
        List<String> recommendedFacilities,
        String nudgeMessage) {
}
