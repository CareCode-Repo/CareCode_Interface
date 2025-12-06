package com.carecode.domain.policy.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/**
 * 정책 상세 정보 응답
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PolicyDetailResponse {
    private PolicyInfoResponse policy;
    private List<PolicyInfoResponse> relatedPolicies;
    private List<String> tags;
    private Map<String, Object> additionalInfo;
    private boolean isBookmarked;
    private int viewCount;
}

