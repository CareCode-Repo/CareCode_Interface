package com.carecode.domain.careFacility.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 보육시설 검색 요청
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CareFacilitySearchRequest {
    private String keyword;
    private String facilityType;
    private String city;
    private String district;
    private Double latitude;
    private Double longitude;
    private Double radius;
    private String sortBy;
    private String sortDirection;
    private int page;
    private int size;
}

