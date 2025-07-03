package com.carecode.domain.policy.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 정책 정보 전송 객체
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PolicyDto {
    
    @NotNull(message = "정책 ID는 필수입니다")
    private Long id;
    
    @NotBlank(message = "정책 제목은 필수입니다")
    @Size(max = 200, message = "정책 제목은 200자를 초과할 수 없습니다")
    private String title;
    
    @NotBlank(message = "정책 설명은 필수입니다")
    @Size(max = 2000, message = "정책 설명은 2000자를 초과할 수 없습니다")
    private String description;
    
    @NotBlank(message = "정책 카테고리는 필수입니다")
    @Pattern(regexp = "^(EDUCATION|HEALTH|FINANCIAL|SUPPORT|OTHER)$", 
             message = "유효하지 않은 정책 카테고리입니다")
    private String category;
    
    @NotBlank(message = "지역은 필수입니다")
    @Size(max = 100, message = "지역명은 100자를 초과할 수 없습니다")
    private String location;
    
    @Min(value = 0, message = "최소 연령은 0 이상이어야 합니다")
    @Max(value = 18, message = "최소 연령은 18 이하여야 합니다")
    private Integer minAge;
    
    @Min(value = 0, message = "최대 연령은 0 이상이어야 합니다")
    @Max(value = 18, message = "최대 연령은 18 이하여야 합니다")
    private Integer maxAge;
    
    @Size(max = 500, message = "지원 금액 설명은 500자를 초과할 수 없습니다")
    private String supportAmount;
    
    @Size(max = 200, message = "신청 기간은 200자를 초과할 수 없습니다")
    private String applicationPeriod;
    
    @Size(max = 1000, message = "자격 기준은 1000자를 초과할 수 없습니다")
    private String eligibilityCriteria;
    
    @Size(max = 500, message = "신청 방법은 500자를 초과할 수 없습니다")
    private String applicationMethod;
    
    @Size(max = 1000, message = "필요 서류는 1000자를 초과할 수 없습니다")
    private String requiredDocuments;
    
    @Size(max = 200, message = "연락처 정보는 200자를 초과할 수 없습니다")
    private String contactInfo;
    
    @Pattern(regexp = "^(https?://)?([\\da-z.-]+)\\.([a-z.]{2,6})[/\\w .-]*/?$", 
             message = "유효하지 않은 웹사이트 URL입니다")
    private String websiteUrl;
    
    @Min(value = 0, message = "조회수는 0 이상이어야 합니다")
    private Integer viewCount;
    
    @NotNull(message = "활성화 상태는 필수입니다")
    private Boolean isActive;
    
    private LocalDateTime createdAt;
    
    private String policyType;
    private Integer targetAgeMin;
    private Integer targetAgeMax;
    private String targetRegion;
    private Integer benefitAmount;
    private String benefitType;
    private LocalDate applicationStartDate;
    private LocalDate applicationEndDate;
    private LocalDate policyStartDate;
    private LocalDate policyEndDate;
    private String applicationUrl;
    private String contactInfo;
    private String requiredDocuments;
    private Boolean isActive;
    private Integer priority;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
} 