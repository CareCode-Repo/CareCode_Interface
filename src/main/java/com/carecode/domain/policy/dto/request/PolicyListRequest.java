package com.carecode.domain.policy.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 정책 목록 조회 요청
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PolicyListRequest {
    private String category;
    private String city;
    private String district;
    private String sortBy;
    private String sortDirection;
    private int page;
    private int size;
}

