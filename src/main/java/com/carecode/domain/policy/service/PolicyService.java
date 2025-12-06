package com.carecode.domain.policy.service;

import com.carecode.core.annotation.CacheableResult;
import com.carecode.core.annotation.LogExecutionTime;
import com.carecode.core.exception.PolicyNotFoundException;
import com.carecode.domain.policy.dto.response.*;
import com.carecode.domain.policy.dto.request.PolicySearchRequest;
import com.carecode.domain.policy.entity.Policy;
import com.carecode.domain.policy.entity.PolicyCategory;
import com.carecode.domain.policy.repository.PolicyRepository;
import com.carecode.domain.policy.mapper.PolicyMapper;
import com.carecode.domain.policy.dto.response.PolicyResponse;
import com.carecode.domain.policy.dto.response.PolicyListResponse;
import com.carecode.domain.policy.dto.response.PolicyInfoResponse;
import com.carecode.domain.policy.dto.response.PolicyStatsSimpleResponse;
import com.carecode.domain.policy.dto.response.PolicyCategoryStatsResponse;
import com.carecode.domain.policy.dto.response.PolicyDto;
import com.carecode.domain.policy.repository.PolicyCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final PolicyMapper policyMapper;

    /**
     * 정책 목록 조회
     */
    @LogExecutionTime
    public List<PolicyDto> getAllPolicies() {
        log.info("전체 정책 목록 조회");
        
        List<Policy> policies = policyRepository.findAll();
        return policies.stream()
                .map(policyMapper::toResponse)
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
        
        return policyMapper.toResponse(policy);
    }

    /**
     * 정책 검색
     */
    @LogExecutionTime
    public PolicyListResponse searchPolicies(PolicySearchRequest request) {
        log.info("정책 검색: 키워드={}, 카테고리={}, 지역={}", 
                request.getKeyword(), request.getCategory(), request.getLocation());
        
        Sort sort = com.carecode.core.util.SortUtil.createSort(
                request.getSortBy(), 
                request.getSortDirection(), 
                "createdAt", 
                Sort.Direction.DESC
        );
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), sort);
        
        Page<Policy> policyPage = policyRepository.findBySearchCriteria(
                request.getKeyword(),
                request.getCategory(),
                request.getCity(),
                null,
                null,
                pageable
        );
        
        List<PolicyDto> policies = policyPage.getContent().stream()
                .map(policyMapper::toResponse)
                .collect(Collectors.toList());

        List<PolicyInfoResponse> policiesResponse = policies.stream()
                .map(dto -> PolicyInfoResponse.builder()
                        .id(dto.getId() != null ? dto.getId().toString() : null)
                        .title(dto.getTitle())
                        .description(dto.getDescription())
                        .category(dto.getCategory())
                        .subCategory(null)
                        .city(null)
                        .district(dto.getLocation())
                        .targetAge(null)
                        .incomeLevel(null)
                        .benefitAmount(dto.getSupportAmount() != null ? dto.getSupportAmount().toString() : null)
                        .applicationMethod(dto.getApplicationMethod())
                        .requiredDocuments(dto.getRequiredDocuments())
                        .contactInfo(dto.getContactInfo())
                        .startDate(null)
                        .endDate(null)
                        .status(Boolean.TRUE.equals(dto.getIsActive()) ? "ACTIVE" : "INACTIVE")
                        .viewCount(dto.getViewCount() != null ? dto.getViewCount() : 0)
                        .createdAt(dto.getCreatedAt())
                        .updatedAt(dto.getUpdatedAt())
                        .build())
                .collect(Collectors.toList());
        
        return PolicyListResponse.builder()
                .policies(policiesResponse)
                .totalCount(policyPage.getTotalElements())
                .currentPage(policyPage.getNumber())
                .totalPages(policyPage.getTotalPages())
                .hasNext(policyPage.hasNext())
                .hasPrevious(policyPage.hasPrevious())
                .category(request.getCategory())
                .city(request.getCity())
                .district(request.getDistrict())
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
                .map(policyMapper::toResponse)
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
                .map(policyMapper::toResponse)
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
                .map(policyMapper::toResponse)
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
                .map(policyMapper::toResponse)
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
                .map(policyMapper::toResponse)
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
    public PolicyStatsSimpleResponse getPolicyStats() {
        log.info("정책 통계 조회");

        long totalPolicies = policyRepository.count();
        long totalViews = policyRepository.getTotalViewCount();
        List<PolicyCategoryStatsResponse> categoryStats = policyRepository.getCategoryStats();

        return PolicyStatsSimpleResponse.builder()
                .totalPolicies(totalPolicies)
                .totalViews(totalViews)
                .categoryStats(categoryStats)
                .build();
    }

    /**
     * 아이 연령별 정책 조회
     */
    @LogExecutionTime
    public List<PolicyDto> getPoliciesByChildAge(Integer childAge) {
        log.info("아이 연령별 정책 조회: 아이 연령={}", childAge);
        
        List<Policy> policies = policyRepository.findByChildAge(childAge);
        return policies.stream()
                .map(policyMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 신청 기간이 유효한 정책 조회
     */
    @LogExecutionTime
    public List<PolicyDto> getActivePoliciesByDate() {
        log.info("신청 기간이 유효한 정책 조회");
        
        List<Policy> policies = policyRepository.findActivePoliciesByDate(java.time.LocalDate.now());
        return policies.stream()
                .map(policyMapper::toResponse)
                .collect(Collectors.toList());
    }

    
    // ========== 정책 카테고리 관련 메서드 ==========
    
    /**
     * 정책 카테고리 목록 조회
     */
    @LogExecutionTime
    public List<PolicyDto> getAllPolicyCategories() {
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
    public PolicyDto getPolicyCategoryById(Long categoryId) {
        log.info("정책 카테고리 상세 조회: 카테고리ID={}", categoryId);
        
        PolicyCategory category = policyCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new PolicyNotFoundException("정책 카테고리를 찾을 수 없습니다: " + categoryId));
        
        return convertToCategoryDto(category);
    }
    
    /**
     * 정책 카테고리 생성
     */
    @Transactional
    public PolicyDto createPolicyCategory(PolicyDto request) {
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
    public PolicyDto updatePolicyCategory(Long categoryId, PolicyDto request) {
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
                .map(policyMapper::toResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * PolicyCategory를 DTO로 변환
     */
    private PolicyDto convertToCategoryDto(PolicyCategory category) {
        return PolicyDto.builder()
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