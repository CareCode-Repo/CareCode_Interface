package com.carecode.domain.policy.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PolicyDto {
    private Long id;
    private String title;
    private String description;
    private String category;
    private String location;
    private String name;
    private Integer displayOrder;
    private Integer minAge;
    private Integer maxAge;
    private Integer supportAmount;
    private String applicationPeriod;
    private String eligibilityCriteria;
    private String applicationMethod;
    private String requiredDocuments;
    private String contactInfo;
    private String websiteUrl;
    private Integer viewCount;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}


