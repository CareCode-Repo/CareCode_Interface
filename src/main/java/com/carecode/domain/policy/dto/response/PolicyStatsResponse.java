package com.carecode.domain.policy.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/**
 * 정책 통계 응답
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PolicyStatsResponse {
    private long totalPolicies;
    private long activePolicies;
    private long expiredPolicies;
    private Map<String, Long> categoryDistribution;
    private Map<String, Long> cityDistribution;
    private long todayViews;
    private long thisWeekViews;
    private long thisMonthViews;
    private List<String> popularPolicies;
}

