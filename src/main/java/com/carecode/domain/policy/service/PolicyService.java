package com.carecode.domain.policy.service;

import com.carecode.core.annotation.CacheableResult;
import com.carecode.core.annotation.LogExecutionTime;
import com.carecode.core.exception.PolicyNotFoundException;
import com.carecode.domain.policy.dto.*;
import com.carecode.domain.policy.entity.Policy;
import com.carecode.domain.policy.entity.PolicyCategory;
import com.carecode.domain.policy.repository.PolicyRepository;
import com.carecode.domain.policy.repository.PolicyCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 정책 서비스 클래스
 * 육아 지원 정책 관련 비즈니스 로직 처리
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PolicyService {

    private final PolicyRepository policyRepository;
    private final PolicyCategoryRepository policyCategoryRepository;

    /**
     * 정책 목록 조회
     */
    @LogExecutionTime
    public List<PolicyDto> getAllPolicies() {
        log.info("전체 정책 목록 조회");
        
        List<Policy> policies = policyRepository.findAll();
        return policies.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * 정책 상세 조회
     */
    @LogExecutionTime
    @CacheableResult(cacheName = "policy", key = "#policyId")
    public PolicyDto getPolicyById(Long policyId) {
        log.info("정책 상세 조회: 정책ID={}", policyId);
        
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new PolicyNotFoundException("정책을 찾을 수 없습니다: " + policyId));
        
        return convertToDto(policy);
    }

    /**
     * 정책 검색
     */
    @LogExecutionTime
    public PolicySearchResponseDto searchPolicies(PolicySearchRequestDto request) {
        log.info("정책 검색: 키워드={}, 카테고리={}, 지역={}", 
                request.getKeyword(), request.getCategory(), request.getLocation());
        
        Pageable pageable = PageRequest.of(
                request.getPage() != null ? request.getPage() : 0, 
                request.getSize() != null ? request.getSize() : 10, 
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
        
        Page<Policy> policyPage = policyRepository.findBySearchCriteria(
                request.getKeyword(),
                request.getCategory(),
                request.getLocation(),
                request.getMinAge(),
                request.getMaxAge(),
                pageable
        );
        
        List<PolicyDto> policies = policyPage.getContent().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        
        return PolicySearchResponseDto.builder()
                .policies(policies)
                .totalElements(policyPage.getTotalElements())
                .totalPages(policyPage.getTotalPages())
                .currentPage(policyPage.getNumber())
                .pageSize(policyPage.getSize())
                .hasNext(policyPage.hasNext())
                .hasPrevious(policyPage.hasPrevious())
                .build();
    }

    /**
     * 카테고리별 정책 조회
     */
    @LogExecutionTime
    public List<PolicyDto> getPoliciesByCategory(String category) {
        log.info("카테고리별 정책 조회: 카테고리={}", category);
        
        List<Policy> policies = policyRepository.findByPolicyType(category);
        return policies.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * 지역별 정책 조회
     */
    @LogExecutionTime
    public List<PolicyDto> getPoliciesByLocation(String location) {
        log.info("지역별 정책 조회: 지역={}", location);
        
        List<Policy> policies = policyRepository.findByTargetRegion(location);
        return policies.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * 연령대별 정책 조회
     */
    @LogExecutionTime
    public List<PolicyDto> getPoliciesByAgeRange(int minAge, int maxAge) {
        log.info("연령대별 정책 조회: 최소연령={}, 최대연령={}", minAge, maxAge);
        
        List<Policy> policies = policyRepository.findByAgeRange(minAge, maxAge);
        return policies.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * 인기 정책 조회 (우선순위 기준)
     */
    @LogExecutionTime
    public List<PolicyDto> getPopularPolicies(int limit) {
        log.info("인기 정책 조회: 제한={}", limit);
        
        Pageable pageable = PageRequest.of(0, limit);
        List<Policy> policies = policyRepository.findPopularPolicies(pageable);
        return policies.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * 최신 정책 조회
     */
    @LogExecutionTime
    public List<PolicyDto> getLatestPolicies(int limit) {
        log.info("최신 정책 조회: 제한={}", limit);
        
        Pageable pageable = PageRequest.of(0, limit);
        List<Policy> policies = policyRepository.findLatestPolicies(pageable);
        return policies.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * 정책 조회수 증가 (현재는 구현하지 않음 - viewCount 필드가 없음)
     */
    @Transactional
    public void incrementViewCount(Long policyId) {
        log.info("정책 조회수 증가: 정책ID={}", policyId);
        
        // viewCount 필드가 엔티티에 없으므로 현재는 로그만 출력
        // 추후 viewCount 필드 추가 시 구현
        log.warn("viewCount 필드가 엔티티에 없어 조회수 증가 기능이 구현되지 않았습니다.");
    }

    /**
     * 정책 카테고리 목록 조회
     */
    @LogExecutionTime
    public List<String> getPolicyCategories() {
        log.info("정책 카테고리 목록 조회");
        
        return policyRepository.findAll().stream()
                .map(Policy::getPolicyType)
                .distinct()
                .filter(type -> type != null && !type.trim().isEmpty())
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * 정책 통계 조회
     */
    @LogExecutionTime
    public PolicySearchResponseDto.PolicyStats getPolicyStats() {
        log.info("정책 통계 조회");
        
        long totalPolicies = policyRepository.count();
        long totalViews = policyRepository.getTotalViewCount();
        List<CategoryStats> categoryStats = policyRepository.getCategoryStats();
        
        return PolicySearchResponseDto.PolicyStats.builder()
                .totalPolicies(totalPolicies)
                .totalViews(totalViews)
                .categoryStats(categoryStats)
                .build();
    }

    /**
     * Entity를 DTO로 변환
     */
    private PolicyDto convertToDto(Policy policy) {
        return PolicyDto.builder()
                .id(policy.getId())
                .title(policy.getTitle())
                .description(policy.getDescription())
                .category(policy.getPolicyCategory() != null ? policy.getPolicyCategory().getName() : policy.getPolicyType())
                .location(policy.getTargetRegion()) // location 필드가 없으므로 targetRegion 사용
                .minAge(policy.getTargetAgeMin()) // minAge 필드가 없으므로 targetAgeMin 사용
                .maxAge(policy.getTargetAgeMax()) // maxAge 필드가 없으므로 targetAgeMax 사용
                .supportAmount(policy.getBenefitAmount()) // supportAmount 필드가 없으므로 benefitAmount 사용
                .applicationPeriod(formatApplicationPeriod(policy.getApplicationStartDate(), policy.getApplicationEndDate()))
                .eligibilityCriteria(null) // eligibilityCriteria 필드가 엔티티에 없음
                .applicationMethod(policy.getApplicationUrl()) // applicationMethod 필드가 없으므로 applicationUrl 사용
                .requiredDocuments(policy.getRequiredDocuments())
                .contactInfo(policy.getContactInfo())
                .websiteUrl(formatWebsiteUrl(policy.getApplicationUrl())) // websiteUrl 필드가 없으므로 applicationUrl 사용
                .viewCount(0) // viewCount 필드가 엔티티에 없으므로 0으로 설정
                .isActive(policy.getIsActive())
                .createdAt(policy.getCreatedAt())
                .updatedAt(policy.getUpdatedAt())
                .build();
    }
    
    /**
     * 신청 기간 포맷팅
     */
    private String formatApplicationPeriod(LocalDate startDate, LocalDate endDate) {
        if (startDate == null && endDate == null) {
            return "상시 신청";
        } else if (startDate == null) {
            return "~ " + endDate.format(DateTimeFormatter.ofPattern("yyyy.MM.dd"));
        } else if (endDate == null) {
            return startDate.format(DateTimeFormatter.ofPattern("yyyy.MM.dd")) + " ~";
        } else {
            return startDate.format(DateTimeFormatter.ofPattern("yyyy.MM.dd")) + " ~ " + endDate.format(DateTimeFormatter.ofPattern("yyyy.MM.dd"));
        }
    }
    
    /**
     * 웹사이트 URL 포맷팅
     */
    private String formatWebsiteUrl(String applicationUrl) {
        if (applicationUrl == null || applicationUrl.trim().isEmpty()) {
            return "해당 정책 홈페이지";
        }
        return applicationUrl;
    }
    
    // ========== 정책 카테고리 관련 메서드 ==========
    
    /**
     * 정책 카테고리 목록 조회
     */
    @LogExecutionTime
    public List<PolicyCategoryDto> getAllPolicyCategories() {
        log.info("정책 카테고리 목록 조회");
        
        List<PolicyCategory> categories = policyCategoryRepository.findByIsActiveTrueOrderByDisplayOrderAsc();
        return categories.stream()
                .map(this::convertToCategoryDto)
                .collect(Collectors.toList());
    }
    
    /**
     * 정책 카테고리 상세 조회
     */
    @LogExecutionTime
    public PolicyCategoryDto getPolicyCategoryById(Long categoryId) {
        log.info("정책 카테고리 상세 조회: 카테고리ID={}", categoryId);
        
        PolicyCategory category = policyCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new PolicyNotFoundException("정책 카테고리를 찾을 수 없습니다: " + categoryId));
        
        return convertToCategoryDto(category);
    }
    
    /**
     * 정책 카테고리 생성
     */
    @Transactional
    public PolicyCategoryDto createPolicyCategory(PolicyCategoryDto request) {
        log.info("정책 카테고리 생성: 이름={}", request.getName());
        
        if (policyCategoryRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("이미 존재하는 카테고리입니다: " + request.getName());
        }
        
        PolicyCategory category = PolicyCategory.builder()
                .name(request.getName())
                .description(request.getDescription())
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                .build();
        
        PolicyCategory savedCategory = policyCategoryRepository.save(category);
        return convertToCategoryDto(savedCategory);
    }
    
    /**
     * 정책 카테고리 수정
     */
    @Transactional
    public PolicyCategoryDto updatePolicyCategory(Long categoryId, PolicyCategoryDto request) {
        log.info("정책 카테고리 수정: 카테고리ID={}", categoryId);
        
        PolicyCategory category = policyCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new PolicyNotFoundException("정책 카테고리를 찾을 수 없습니다: " + categoryId));
        
        category.updateCategory(request.getName(), request.getDescription(), request.getDisplayOrder());
        PolicyCategory updatedCategory = policyCategoryRepository.save(category);
        
        return convertToCategoryDto(updatedCategory);
    }
    
    /**
     * 정책 카테고리 삭제
     */
    @Transactional
    public void deletePolicyCategory(Long categoryId) {
        log.info("정책 카테고리 삭제: 카테고리ID={}", categoryId);
        
        PolicyCategory category = policyCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new PolicyNotFoundException("정책 카테고리를 찾을 수 없습니다: " + categoryId));
        
        category.deactivate();
        policyCategoryRepository.save(category);
    }
    
    /**
     * 카테고리별 정책 조회 (정규화된 구조)
     */
    @LogExecutionTime
    public List<PolicyDto> getPoliciesByCategoryId(Long categoryId) {
        log.info("카테고리별 정책 조회: 카테고리ID={}", categoryId);
        
        PolicyCategory category = policyCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new PolicyNotFoundException("정책 카테고리를 찾을 수 없습니다: " + categoryId));
        
        List<Policy> policies = category.getPolicies();
        return policies.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    /**
     * PolicyCategory를 DTO로 변환
     */
    private PolicyCategoryDto convertToCategoryDto(PolicyCategory category) {
        return PolicyCategoryDto.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .displayOrder(category.getDisplayOrder())
                .isActive(category.getIsActive())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }
}