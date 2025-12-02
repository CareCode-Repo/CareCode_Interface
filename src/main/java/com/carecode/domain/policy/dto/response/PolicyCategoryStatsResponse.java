package com.carecode.domain.policy.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 카테고리별 통계 응답
 */
@Getter
@Setter
@NoArgsConstructor
@Builder
public class PolicyCategoryStatsResponse {
    private String category;
    private long count;
    private double averageRating;
    private long totalViews;
    
    // JPA 쿼리에서 사용할 생성자
    public PolicyCategoryStatsResponse(String category, long count, double averageRating, long totalViews) {
        this.category = category;
        this.count = count;
        this.averageRating = averageRating;
        this.totalViews = totalViews;
    }
}

