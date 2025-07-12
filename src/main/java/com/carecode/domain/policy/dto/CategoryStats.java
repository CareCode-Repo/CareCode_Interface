package com.carecode.domain.policy.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * 카테고리별 통계 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@Builder
public class CategoryStats {
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