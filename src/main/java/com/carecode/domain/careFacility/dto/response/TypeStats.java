package com.carecode.domain.careFacility.dto.response;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.Builder;
import com.carecode.domain.careFacility.entity.FacilityType;

/**
 * 시설 유형별 통계 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@Builder
public class TypeStats {
    private FacilityType facilityType;
    private long count;
    private double averageRating;
    private long totalViews;
    
    // JPA 쿼리에서 사용할 생성자
    public TypeStats(FacilityType facilityType, long count, double averageRating, long totalViews) {
        this.facilityType = facilityType;
        this.count = count;
        this.averageRating = averageRating;
        this.totalViews = totalViews;
    }
} 