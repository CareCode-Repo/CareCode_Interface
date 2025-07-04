package com.carecode.domain.careFacility.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;

/**
 * 육아 시설 검색 응답 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CareFacilitySearchResponseDto {
    
    private List<CareFacilityDto> facilities;
    private long totalElements;
    private int totalPages;
    private int currentPage;
    private int pageSize;
    private boolean hasNext;
    private boolean hasPrevious;
    
    /**
     * 시설 통계 정보 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FacilityStats {
        private long totalFacilities;
        private long totalViews;
        private List<TypeStats> typeStats;
    }
    
    /**
     * 시설 유형별 통계 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TypeStats {
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
} 