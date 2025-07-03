package com.carecode.domain.policy.service;

import com.carecode.core.annotation.CacheableResult;
import com.carecode.core.annotation.LogExecutionTime;
import com.carecode.core.exception.PolicyNotFoundException;
import com.carecode.domain.policy.dto.PolicyDto;
import com.carecode.domain.policy.dto.PolicySearchRequestDto;
import com.carecode.domain.policy.dto.PolicySearchResponseDto;
import com.carecode.domain.policy.entity.Policy;
import com.carecode.domain.policy.repository.PolicyRepository;
import com.carecode.core.util.PolicyUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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
                request.getPage(), 
                request.getSize(), 
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
                .size(policyPage.getSize())
                .build();
    }

    /**
     * 카테고리별 정책 조회
     */
    @LogExecutionTime
    public List<PolicyDto> getPoliciesByCategory(String category) {
        log.info("카테고리별 정책 조회: 카테고리={}", category);
        
        List<Policy> policies = policyRepository.findByCategory(category);
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
        
        List<Policy> policies = policyRepository.findByLocation(location);
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
     * 인기 정책 조회 (조회수 기준)
     */
    @LogExecutionTime
    public List<PolicyDto> getPopularPolicies(int limit) {
        log.info("인기 정책 조회: 제한={}", limit);
        
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, limit);
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
        
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, limit);
        List<Policy> policies = policyRepository.findLatestPolicies(pageable);
        return policies.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * 정책 조회수 증가
     */
    @Transactional
    public void incrementViewCount(Long policyId) {
        log.info("정책 조회수 증가: 정책ID={}", policyId);
        
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new PolicyNotFoundException("정책을 찾을 수 없습니다: " + policyId));
        
        policy.setViewCount(policy.getViewCount() + 1);
        policyRepository.save(policy);
    }

    /**
     * 정책 통계 조회
     */
    @LogExecutionTime
    public PolicySearchResponseDto.PolicyStats getPolicyStats() {
        log.info("정책 통계 조회");
        
        long totalPolicies = policyRepository.count();
        long totalViews = policyRepository.getTotalViewCount();
        List<PolicySearchResponseDto.CategoryStats> categoryStats = policyRepository.getCategoryStats();
        
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
                .category(policy.getCategory())
                .location(policy.getLocation())
                .minAge(policy.getMinAge())
                .maxAge(policy.getMaxAge())
                .supportAmount(policy.getSupportAmount())
                .applicationPeriod(policy.getApplicationPeriod())
                .eligibilityCriteria(policy.getEligibilityCriteria())
                .applicationMethod(policy.getApplicationMethod())
                .requiredDocuments(policy.getRequiredDocuments())
                .contactInfo(policy.getContactInfo())
                .websiteUrl(policy.getWebsiteUrl())
                .viewCount(policy.getViewCount())
                .isActive(policy.getIsActive())
                .createdAt(policy.getCreatedAt())
                .updatedAt(policy.getUpdatedAt())
                .build();
    }
} 