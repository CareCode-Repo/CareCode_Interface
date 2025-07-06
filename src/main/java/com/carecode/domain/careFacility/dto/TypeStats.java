package com.carecode.domain.careFacility.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.Builder;

/**
 * 시설 유형별 통계 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@Builder
public class TypeStats {
    private String facilityType;
    private long count;
    private double averageRating;
    private long totalViews;
    
    // JPA 쿼리에서 사용할 생성자
    public TypeStats(String facilityType, long count, double averageRating, long totalViews) {
        this.facilityType = facilityType;
        this.count = count;
        this.averageRating = averageRating;
        this.totalViews = totalViews;
    }
} 