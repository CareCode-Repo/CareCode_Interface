package com.carecode.domain.policy.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 정책 카테고리 응답
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PolicyCategoryResponse {
    private String id;
    private String name;
    private String description;
    private String parentCategory;
    private List<PolicyCategoryResponse> subCategories;
    private int policyCount;
    private String icon;
    private String color;
}

