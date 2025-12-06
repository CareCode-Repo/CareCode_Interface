package com.carecode.domain.policy.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 정책 생성 요청
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PolicyCreateRequest {
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

