package com.carecode.domain.policy.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 정책 카테고리 조회 요청
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PolicyCategoryRequest {
    private String parentCategory;
    private boolean includeSubCategories;
}

