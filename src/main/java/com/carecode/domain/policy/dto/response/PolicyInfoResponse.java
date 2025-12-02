package com.carecode.domain.policy.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 정책 정보 응답
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PolicyInfoResponse {
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

