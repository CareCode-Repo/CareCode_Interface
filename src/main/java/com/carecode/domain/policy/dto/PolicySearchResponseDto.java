package com.carecode.domain.policy.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;

/**
 * 정책 검색 응답 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PolicySearchResponseDto {
    
    private List<PolicyDto> policies;
    private long totalElements;
    private int totalPages;
    private int currentPage;
    private int pageSize;
    private boolean hasNext;
    private boolean hasPrevious;
    
    /**
     * 정책 통계 정보 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PolicyStats {
        private long totalPolicies;
        private long totalViews;
        private List<CategoryStats> categoryStats;
    }
    
    /**
     * 카테고리별 통계 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CategoryStats {
        private String category;
        private long count;
        private double averageRating;
        private long totalViews;
        
        // JPA 쿼리에서 사용할 생성자
        public CategoryStats(String category, long count, double averageRating, long totalViews) {
            this.category = category;
            this.count = count;
            this.averageRating = averageRating;
            this.totalViews = totalViews;
        }
    }
} 