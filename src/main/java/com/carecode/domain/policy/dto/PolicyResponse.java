package com.carecode.domain.policy.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 정책 관련 응답 DTO 통합 클래스
 */
public class PolicyResponse {

    /**
     * 정책 정보 응답
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Policy {
        private String id;
        private String title;
        private String description;
        private String category;
        private String subCategory;
        private String city;
        private String district;
        private String targetAge;
        private String incomeLevel;
        private String benefitAmount;
        private String applicationMethod;
        private String requiredDocuments;
        private String contactInfo;
        private LocalDate startDate;
        private LocalDate endDate;
        private String status;
        private int viewCount;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    /**
     * 정책 카테고리 응답
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PolicyCategory {
        private String id;
        private String name;
        private String description;
        private String parentCategory;
        private List<PolicyCategory> subCategories;
        private int policyCount;
        private String icon;
        private String color;
    }

    /**
     * 정책 검색 응답
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PolicySearch {
        private List<Policy> policies;
        private long totalCount;
        private String searchKeyword;
        private List<String> searchFilters;
        private String sortBy;
        private String sortDirection;
    }

    /**
     * 정책 목록 응답
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PolicyList {
        private List<Policy> policies;
        private long totalCount;
        private int currentPage;
        private int totalPages;
        private boolean hasNext;
        private boolean hasPrevious;
        private String category;
        private String city;
        private String district;
    }

    /**
     * 정책 상세 정보 응답
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PolicyDetail {
        private Policy policy;
        private List<Policy> relatedPolicies;
        private List<String> tags;
        private Map<String, Object> additionalInfo;
        private boolean isBookmarked;
        private int viewCount;
    }

    /**
     * 정책 통계 응답
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PolicyStats {
        private long totalPolicies;
        private long activePolicies;
        private long expiredPolicies;
        private Map<String, Long> categoryDistribution;
        private Map<String, Long> cityDistribution;
        private long todayViews;
        private long thisWeekViews;
        private long thisMonthViews;
        private List<String> popularPolicies;
    }

    /**
     * 정책 카테고리 목록 응답
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PolicyCategoryList {
        private List<PolicyCategory> categories;
        private long totalCount;
        private List<String> popularCategories;
    }

    /**
     * 정책 북마크 응답
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PolicyBookmark {
        private String policyId;
        private String userId;
        private String title;
        private String category;
        private LocalDateTime bookmarkedAt;
    }

    /**
     * 정책 북마크 목록 응답
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PolicyBookmarkList {
        private List<PolicyBookmark> bookmarks;
        private long totalCount;
        private int currentPage;
        private int totalPages;
        private boolean hasNext;
        private boolean hasPrevious;
    }

    /**
     * 정책 추천 응답
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PolicyRecommendation {
        private List<Policy> recommendedPolicies;
        private String recommendationReason;
        private Map<String, Object> userProfile;
        private List<String> matchingCriteria;
    }

    /**
     * 정책 카테고리 정보 응답
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CategoryInfo {
        private Long id;
        private String name;
        private String description;
        private Integer displayOrder;
        private Boolean isActive;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    /**
     * 정책 통계 응답
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Stats {
        private long totalPolicies;
        private long totalViews;
        private List<CategoryStats> categoryStats;
    }

    /**
     * 정책 북마크 응답
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BookmarkInfo {
        private Long id;
        private String userId;
        private Long policyId;
        private LocalDateTime createdAt;
    }

    /**
     * 카테고리별 통계 응답
     */
    @Getter
    @Setter
    @NoArgsConstructor
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
