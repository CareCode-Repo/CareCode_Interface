package com.carecode.domain.policy.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * 정책 관련 요청 DTO 통합 클래스
 */
public class PolicyRequest {

    /**
     * 정책 검색 요청
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Search {
        private String keyword;
        private String category;
        private String city;
        private String district;
        private String targetAge;
        private String incomeLevel;
        private String location;
        private LocalDate startDate;
        private LocalDate endDate;
        private String sortBy;
        private String sortDirection;
        private int page;
        private int size;
    }

    /**
     * 정책 목록 조회 요청
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PolicyListRequest {
        private String category;
        private String city;
        private String district;
        private String sortBy;
        private String sortDirection;
        private int page;
        private int size;
    }

    /**
     * 정책 카테고리 조회 요청
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Category {
        private String parentCategory;
        private boolean includeSubCategories;
    }

    /**
     * 정책 상세 조회 요청
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Detail {
        @NotBlank(message = "정책 ID는 필수입니다")
        private String policyId;
        
        private String userId;
    }

    /**
     * 정책 생성 요청
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Create {
        @NotBlank(message = "정책 제목은 필수입니다")
        private String title;
        
        @NotBlank(message = "정책 설명은 필수입니다")
        private String description;
        
        @NotBlank(message = "정책 카테고리는 필수입니다")
        private String category;
        
        @NotBlank(message = "지역은 필수입니다")
        private String location;
        
        private Integer minAge;
        private Integer maxAge;
        private Integer supportAmount;
        private String applicationPeriod;
        private String eligibilityCriteria;
        private String applicationMethod;
        private String requiredDocuments;
        private String contactInfo;
        private String websiteUrl;
    }

    /**
     * 정책 수정 요청
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Update {
        @NotBlank(message = "정책 제목은 필수입니다")
        private String title;
        
        @NotBlank(message = "정책 설명은 필수입니다")
        private String description;
        
        @NotBlank(message = "정책 카테고리는 필수입니다")
        private String category;
        
        @NotBlank(message = "지역은 필수입니다")
        private String location;
        
        private Integer minAge;
        private Integer maxAge;
        private Integer supportAmount;
        private String applicationPeriod;
        private String eligibilityCriteria;
        private String applicationMethod;
        private String requiredDocuments;
        private String contactInfo;
        private String websiteUrl;
        private Boolean isActive;
    }

    /**
     * 정책 삭제 요청
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Delete {
        @NotBlank(message = "정책 ID는 필수입니다")
        private Long policyId;
    }

    /**
     * 정책 북마크 요청
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Bookmark {
        @NotBlank(message = "사용자 ID는 필수입니다")
        private String userId;
        
        @NotBlank(message = "정책 ID는 필수입니다")
        private Long policyId;
    }
}
