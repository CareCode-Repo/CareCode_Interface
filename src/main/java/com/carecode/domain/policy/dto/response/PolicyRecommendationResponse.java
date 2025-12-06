package com.carecode.domain.policy.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/**
 * 정책 추천 응답
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PolicyRecommendationResponse {
    private List<PolicyInfoResponse> recommendedPolicies;
    private String recommendationReason;
    private Map<String, Object> userProfile;
    private List<String> matchingCriteria;
}

