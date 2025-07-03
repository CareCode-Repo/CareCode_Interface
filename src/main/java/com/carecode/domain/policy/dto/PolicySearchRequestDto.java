package com.carecode.domain.policy.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import jakarta.validation.constraints.*;
import java.util.List;

/**
 * 정책 검색 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PolicySearchRequestDto {
    
    @Size(max = 100, message = "검색 키워드는 100자를 초과할 수 없습니다")
    private String keyword;
    
    @Pattern(regexp = "^(EDUCATION|HEALTH|FINANCIAL|SUPPORT|OTHER)$", 
             message = "유효하지 않은 정책 카테고리입니다")
    private String category;
    
    @Size(max = 100, message = "지역명은 100자를 초과할 수 없습니다")
    private String location;
    
    @Min(value = 0, message = "최소 연령은 0 이상이어야 합니다")
    @Max(value = 18, message = "최소 연령은 18 이하여야 합니다")
    private Integer minAge;
    
    @Min(value = 0, message = "최대 연령은 0 이상이어야 합니다")
    @Max(value = 18, message = "최대 연령은 18 이하여야 합니다")
    private Integer maxAge;
    
    @Min(value = 0, message = "페이지 번호는 0 이상이어야 합니다")
    private Integer page = 0;
    
    @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다")
    @Max(value = 100, message = "페이지 크기는 100을 초과할 수 없습니다")
    private Integer size = 10;
    
    @Pattern(regexp = "^(title|category|location|createdAt)$", 
             message = "유효하지 않은 정렬 기준입니다")
    private String sortBy = "createdAt";
    
    @Pattern(regexp = "^(ASC|DESC)$", message = "정렬 방향은 ASC 또는 DESC여야 합니다")
    private String sortDirection = "DESC";
} 