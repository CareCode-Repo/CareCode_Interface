package com.carecode.domain.health.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

/**
 * 병원 수집 현황. 소개 사이트가 "몇 곳을 모았는지" 를 자동으로 가져가는 데 쓴다.
 *
 * <p>목록 API 는 페이지 상한이 있어 세면 실제보다 적게 나온다. 그래서 집계를 따로 준다.
 */
@Getter
@Builder
public class HospitalStatsResponse {

    private final long totalHospitals;

    /** 진료과목(종별) → 병원 수. 값이 비어 있는 병원은 "기타" 로 묶는다. 많은 순. */
    private final Map<String, Long> byType;

    /** 병원 데이터가 마지막으로 갱신된 시각. 동기화 이력이 없으면 null. */
    private final java.time.LocalDateTime dataUpdatedAt;
}
