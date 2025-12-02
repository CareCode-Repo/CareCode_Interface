package com.carecode.domain.policy.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * 정책 검색 요청
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PolicySearchRequest {
    private String keyword;
    private String category;
    private String city;
    private String district;
    private String targetAge;
    private String incomeLevel;
    private String location;
    private LocalDate startDate;
    private LocalDate endDate;
    private String sortBy;
    private String sortDirection;
    private int page;
    private int size;
}

