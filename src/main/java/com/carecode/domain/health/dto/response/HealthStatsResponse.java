package com.carecode.domain.health.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/**
 * 건강 통계 응답
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HealthStatsResponse {
    private Integer totalRecords;
    private Integer completedVaccines;
    private Integer pendingVaccines;
    private Integer completedCheckups;
    private Integer pendingCheckups;
    private Map<String, Integer> recordTypeDistribution;
    private List<String> upcomingEvents;
}

