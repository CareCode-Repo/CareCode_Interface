package com.carecode.domain.careFacility.dto.request;

import com.carecode.domain.careFacility.entity.FacilityType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 고급 시설 검색 요청 DTO
 * 복합 조건으로 시설을 검색합니다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CareFacilityAdvancedSearchRequest {
    private FacilityType facilityType;
    private Boolean isPublic;
    private Boolean subsidyAvailable;
    private Double minRating;
    private Integer minAvailableSpots;
    private Integer maxTuitionFee;
    private Integer childAge;
    private String sortBy;
    private String sortDirection;
}

