package com.carecode.domain.careFacility.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/** 보육시설 통계 응답 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CareFacilityStatsResponse {
    private long totalFacilities;
    private long totalBookings;
    private long activeFacilities;
    private Map<String, Long> typeDistribution;
    private List<TypeStats> typeStats;
    private long todayBookings;
    private long thisWeekBookings;
    private long thisMonthBookings;

    /**
     * 시설 데이터가 마지막으로 갱신된 시각. 소개 사이트가 "○월 ○일 기준" 을 자동으로 표시한다.
     * 동기화 이력이 없으면 null (한 번도 돌지 않았다는 뜻이라 0 이나 현재 시각으로 속이지 않는다).
     */
    private java.time.LocalDateTime dataUpdatedAt;
}

