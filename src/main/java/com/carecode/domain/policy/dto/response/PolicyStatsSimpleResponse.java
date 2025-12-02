package com.carecode.domain.policy.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 정책 통계 응답 (간단 버전)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PolicyStatsSimpleResponse {
    private long totalPolicies;
    private long totalViews;
    private List<PolicyCategoryStatsResponse> categoryStats;
}

