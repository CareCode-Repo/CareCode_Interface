package com.carecode.domain.policy.service;

import com.carecode.core.exception.PolicyNotFoundException;
import com.carecode.domain.policy.dto.PolicyDto;
import com.carecode.domain.policy.dto.PolicySearchRequestDto;
import com.carecode.domain.policy.dto.PolicySearchResponseDto;
import com.carecode.domain.policy.entity.Policy;
import com.carecode.domain.policy.repository.PolicyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PolicyServiceTest {

    @Mock
    private PolicyRepository policyRepository;

    @InjectMocks
    private PolicyService policyService;

    private Policy testPolicy;
    private PolicyDto testPolicyDto;

    @BeforeEach
    void setUp() {
        testPolicy = Policy.builder()
                .id(1L)
                .title("테스트 정책")
                .description("테스트 정책 설명")
                .category("EDUCATION")
                .location("서울시")
                .minAge(3)
                .maxAge(6)
                .supportAmount("월 30만원")
                .applicationPeriod("2024.01.01 ~ 2024.12.31")
                .eligibilityCriteria("만 3-6세 아동")
                .applicationMethod("온라인 신청")
                .requiredDocuments("주민등록등본, 소득증빙서류")
                .contactInfo("02-1234-5678")
                .websiteUrl("https://test.go.kr")
                .viewCount(100)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testPolicyDto = PolicyDto.builder()
                .id(1L)
                .title("테스트 정책")
                .description("테스트 정책 설명")
                .category("EDUCATION")
                .location("서울시")
                .minAge(3)
                .maxAge(6)
                .supportAmount("월 30만원")
                .applicationPeriod("2024.01.01 ~ 2024.12.31")
                .eligibilityCriteria("만 3-6세 아동")
                .applicationMethod("온라인 신청")
                .requiredDocuments("주민등록등본, 소득증빙서류")
                .contactInfo("02-1234-5678")
                .websiteUrl("https://test.go.kr")
                .viewCount(100)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("전체 정책 목록 조회 성공")
    void getAllPolicies_Success() {
        // given
        List<Policy> policies = List.of(testPolicy);
        when(policyRepository.findAll()).thenReturn(policies);

        // when
        List<PolicyDto> result = policyService.getAllPolicies();

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("테스트 정책");
        verify(policyRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("정책 상세 조회 성공")
    void getPolicyById_Success() {
        // given
        when(policyRepository.findById(1L)).thenReturn(Optional.of(testPolicy));

        // when
        PolicyDto result = policyService.getPolicyById(1L);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("테스트 정책");
        assertThat(result.getCategory()).isEqualTo("EDUCATION");
        verify(policyRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("정책 상세 조회 실패 - 존재하지 않는 정책")
    void getPolicyById_NotFound() {
        // given
        when(policyRepository.findById(999L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> policyService.getPolicyById(999L))
                .isInstanceOf(PolicyNotFoundException.class)
                .hasMessage("정책을 찾을 수 없습니다: 999");
        verify(policyRepository, times(1)).findById(999L);
    }

    @Test
    @DisplayName("정책 검색 성공")
    void searchPolicies_Success() {
        // given
        PolicySearchRequestDto request = PolicySearchRequestDto.builder()
                .keyword("교육")
                .category("EDUCATION")
                .location("서울시")
                .minAge(3)
                .maxAge(6)
                .page(0)
                .size(10)
                .build();

        Page<Policy> policyPage = new PageImpl<>(List.of(testPolicy), PageRequest.of(0, 10), 1);
        when(policyRepository.findBySearchCriteria(
                eq("교육"), eq("EDUCATION"), eq("서울시"), eq(3), eq(6), any(Pageable.class)
        )).thenReturn(policyPage);

        // when
        PolicySearchResponseDto result = policyService.searchPolicies(request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getPolicies()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getCurrentPage()).isEqualTo(0);
        verify(policyRepository, times(1)).findBySearchCriteria(
                eq("교육"), eq("EDUCATION"), eq("서울시"), eq(3), eq(6), any(Pageable.class)
        );
    }

    @Test
    @DisplayName("카테고리별 정책 조회 성공")
    void getPoliciesByCategory_Success() {
        // given
        List<Policy> policies = List.of(testPolicy);
        when(policyRepository.findByCategory("EDUCATION")).thenReturn(policies);

        // when
        List<PolicyDto> result = policyService.getPoliciesByCategory("EDUCATION");

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCategory()).isEqualTo("EDUCATION");
        verify(policyRepository, times(1)).findByCategory("EDUCATION");
    }

    @Test
    @DisplayName("지역별 정책 조회 성공")
    void getPoliciesByLocation_Success() {
        // given
        List<Policy> policies = List.of(testPolicy);
        when(policyRepository.findByLocation("서울시")).thenReturn(policies);

        // when
        List<PolicyDto> result = policyService.getPoliciesByLocation("서울시");

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getLocation()).isEqualTo("서울시");
        verify(policyRepository, times(1)).findByLocation("서울시");
    }

    @Test
    @DisplayName("연령대별 정책 조회 성공")
    void getPoliciesByAgeRange_Success() {
        // given
        List<Policy> policies = List.of(testPolicy);
        when(policyRepository.findByAgeRange(3, 6)).thenReturn(policies);

        // when
        List<PolicyDto> result = policyService.getPoliciesByAgeRange(3, 6);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMinAge()).isEqualTo(3);
        assertThat(result.get(0).getMaxAge()).isEqualTo(6);
        verify(policyRepository, times(1)).findByAgeRange(3, 6);
    }

    @Test
    @DisplayName("인기 정책 조회 성공")
    void getPopularPolicies_Success() {
        // given
        List<Policy> policies = List.of(testPolicy);
        when(policyRepository.findPopularPolicies(10)).thenReturn(policies);

        // when
        List<PolicyDto> result = policyService.getPopularPolicies(10);

        // then
        assertThat(result).hasSize(1);
        verify(policyRepository, times(1)).findPopularPolicies(10);
    }

    @Test
    @DisplayName("최신 정책 조회 성공")
    void getLatestPolicies_Success() {
        // given
        List<Policy> policies = List.of(testPolicy);
        when(policyRepository.findLatestPolicies(10)).thenReturn(policies);

        // when
        List<PolicyDto> result = policyService.getLatestPolicies(10);

        // then
        assertThat(result).hasSize(1);
        verify(policyRepository, times(1)).findLatestPolicies(10);
    }

    @Test
    @DisplayName("정책 조회수 증가 성공")
    void incrementViewCount_Success() {
        // given
        when(policyRepository.findById(1L)).thenReturn(Optional.of(testPolicy));
        when(policyRepository.save(any(Policy.class))).thenReturn(testPolicy);

        // when
        policyService.incrementViewCount(1L);

        // then
        verify(policyRepository, times(1)).findById(1L);
        verify(policyRepository, times(1)).save(any(Policy.class));
    }

    @Test
    @DisplayName("정책 조회수 증가 실패 - 존재하지 않는 정책")
    void incrementViewCount_NotFound() {
        // given
        when(policyRepository.findById(999L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> policyService.incrementViewCount(999L))
                .isInstanceOf(PolicyNotFoundException.class)
                .hasMessage("정책을 찾을 수 없습니다: 999");
        verify(policyRepository, times(1)).findById(999L);
        verify(policyRepository, never()).save(any(Policy.class));
    }

    @Test
    @DisplayName("정책 통계 조회 성공")
    void getPolicyStats_Success() {
        // given
        when(policyRepository.count()).thenReturn(100L);
        when(policyRepository.getTotalViewCount()).thenReturn(1000L);
        when(policyRepository.getCategoryStats()).thenReturn(List.of());

        // when
        PolicySearchResponseDto.PolicyStats result = policyService.getPolicyStats();

        // then
        assertThat(result).isNotNull();
        assertThat(result.getTotalPolicies()).isEqualTo(100L);
        assertThat(result.getTotalViews()).isEqualTo(1000L);
        verify(policyRepository, times(1)).count();
        verify(policyRepository, times(1)).getTotalViewCount();
        verify(policyRepository, times(1)).getCategoryStats();
    }
} 