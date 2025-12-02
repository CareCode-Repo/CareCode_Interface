package com.carecode.domain.policy.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 정책 검색 응답
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PolicySearchResponse {
    private List<PolicyInfoResponse> policies;
    private long totalCount;
    private String searchKeyword;
    private List<String> searchFilters;
    private String sortBy;
    private String sortDirection;
}

