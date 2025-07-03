package com.carecode.domain.policy.controller;

import com.carecode.core.annotation.LogExecutionTime;
import com.carecode.core.annotation.ValidateLocation;
import com.carecode.core.controller.BaseController;
import com.carecode.core.exception.CareServiceException;
import com.carecode.core.exception.PolicyNotFoundException;
import com.carecode.domain.policy.dto.PolicyDto;
import com.carecode.domain.policy.dto.PolicySearchRequestDto;
import com.carecode.domain.policy.dto.PolicySearchResponseDto;
import com.carecode.domain.policy.service.PolicyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 육아 정책 API 컨트롤러
 * 정부 육아 정책 정보 제공 및 검색 서비스
 */
@RestController
@RequestMapping("/policies")
@RequiredArgsConstructor
@Slf4j
public class PolicyController extends BaseController {

    private final PolicyService policyService;

    /**
     * 전체 정책 목록 조회
     */
    @GetMapping
    @LogExecutionTime
    public ResponseEntity<List<PolicyDto>> getAllPolicies() {
        log.info("전체 정책 목록 조회");
        List<PolicyDto> policies = policyService.getAllPolicies();
        return ResponseEntity.ok(policies);
    }

    /**
     * 정책 상세 정보 조회
     */
    @GetMapping("/{policyId}")
    @LogExecutionTime
    public ResponseEntity<PolicyDto> getPolicy(@PathVariable Long policyId) {
        log.info("정책 상세 조회: 정책ID={}", policyId);
        
        try {
            PolicyDto policy = policyService.getPolicyById(policyId);
            return ResponseEntity.ok(policy);
        } catch (PolicyNotFoundException e) {
            log.error("정책을 찾을 수 없음: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 정책 검색 (페이징)
     */
    @PostMapping("/search")
    @LogExecutionTime
    public ResponseEntity<PolicySearchResponseDto> searchPolicies(@RequestBody PolicySearchRequestDto request) {
        log.info("정책 검색: 키워드={}, 카테고리={}, 지역={}", 
                request.getKeyword(), request.getCategory(), request.getLocation());
        
        try {
            PolicySearchResponseDto response = policyService.searchPolicies(request);
            return ResponseEntity.ok(response);
        } catch (CareServiceException e) {
            log.error("정책 검색 오류: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 카테고리별 정책 조회
     */
    @GetMapping("/category/{category}")
    @LogExecutionTime
    public ResponseEntity<List<PolicyDto>> getPoliciesByCategory(@PathVariable String category) {
        log.info("카테고리별 정책 조회: 카테고리={}", category);
        
        try {
            List<PolicyDto> policies = policyService.getPoliciesByCategory(category);
            return ResponseEntity.ok(policies);
        } catch (CareServiceException e) {
            log.error("카테고리별 정책 조회 오류: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 지역별 정책 조회
     */
    @GetMapping("/location/{location}")
    @LogExecutionTime
    @ValidateLocation
    public ResponseEntity<List<PolicyDto>> getPoliciesByLocation(@PathVariable String location) {
        log.info("지역별 정책 조회: 지역={}", location);
        
        try {
            List<PolicyDto> policies = policyService.getPoliciesByLocation(location);
            return ResponseEntity.ok(policies);
        } catch (CareServiceException e) {
            log.error("지역별 정책 조회 오류: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 연령대별 정책 조회
     */
    @GetMapping("/age")
    @LogExecutionTime
    public ResponseEntity<List<PolicyDto>> getPoliciesByAgeRange(
            @RequestParam Integer minAge, 
            @RequestParam Integer maxAge) {
        log.info("연령대별 정책 조회: 최소연령={}, 최대연령={}", minAge, maxAge);
        
        try {
            List<PolicyDto> policies = policyService.getPoliciesByAgeRange(minAge, maxAge);
            return ResponseEntity.ok(policies);
        } catch (CareServiceException e) {
            log.error("연령대별 정책 조회 오류: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 인기 정책 조회 (조회수 기준)
     */
    @GetMapping("/popular")
    @LogExecutionTime
    public ResponseEntity<List<PolicyDto>> getPopularPolicies(@RequestParam(defaultValue = "10") Integer limit) {
        log.info("인기 정책 조회: 제한={}", limit);
        
        try {
            List<PolicyDto> policies = policyService.getPopularPolicies(limit);
            return ResponseEntity.ok(policies);
        } catch (CareServiceException e) {
            log.error("인기 정책 조회 오류: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 최신 정책 조회
     */
    @GetMapping("/latest")
    @LogExecutionTime
    public ResponseEntity<List<PolicyDto>> getLatestPolicies(@RequestParam(defaultValue = "10") Integer limit) {
        log.info("최신 정책 조회: 제한={}", limit);
        
        try {
            List<PolicyDto> policies = policyService.getLatestPolicies(limit);
            return ResponseEntity.ok(policies);
        } catch (CareServiceException e) {
            log.error("최신 정책 조회 오류: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 정책 조회수 증가
     */
    @PostMapping("/{policyId}/view")
    @LogExecutionTime
    public ResponseEntity<Map<String, String>> incrementViewCount(@PathVariable Long policyId) {
        log.info("정책 조회수 증가 요청 - ID: {}", policyId);
        
        try {
            policyService.incrementViewCount(policyId);
            return ResponseEntity.ok(Map.of("message", SUCCESS_MESSAGE));
        } catch (PolicyNotFoundException e) {
            log.error("정책을 찾을 수 없음: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 정책 통계 조회
     */
    @GetMapping("/statistics")
    @LogExecutionTime
    public ResponseEntity<PolicySearchResponseDto.PolicyStats> getPolicyStatistics() {
        log.info("정책 통계 조회");
        
        try {
            PolicySearchResponseDto.PolicyStats stats = policyService.getPolicyStats();
            return ResponseEntity.ok(stats);
        } catch (CareServiceException e) {
            log.error("정책 통계 조회 오류: {}", e.getMessage());
            throw e;
        }
    }
} 