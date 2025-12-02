package com.carecode.domain.careFacility.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/**
 * 보육시설 통계 응답
 */
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
}

