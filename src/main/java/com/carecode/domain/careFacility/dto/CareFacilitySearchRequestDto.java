package com.carecode.domain.careFacility.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * 육아 시설 검색 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CareFacilitySearchRequestDto {
    
    private String keyword;
    private String facilityType;
    private String region;
    private Double latitude;
    private Double longitude;
    private Double radiusKm;
    private Integer childAge;
    private Integer minRating;
    private Boolean isPublic;
    private Boolean subsidyAvailable;
    private Integer minAvailableSpots;
    private Integer maxTuitionFee;
    private Integer page;
    private Integer size;
    private String sortBy;
    private String sortDirection;
} 