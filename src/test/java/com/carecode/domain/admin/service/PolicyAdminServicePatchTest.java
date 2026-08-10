package com.carecode.domain.admin.service;

import com.carecode.core.exception.BusinessException;
import com.carecode.domain.admin.dto.AdminPolicyDetailResponse;
import com.carecode.domain.policy.entity.Policy;
import com.carecode.domain.policy.entity.PolicyCategory;
import com.carecode.domain.policy.repository.PolicyCategoryRepository;
import com.carecode.domain.policy.repository.PolicyRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 정책 부분 수정.
 *
 * <p>핵심은 "값이 null" 과 "키가 아예 없음" 을 구분하는 것이다. 이걸 구분하지 못하면
 * 필드를 비우는 것과 건드리지 않는 것 중 하나는 할 수 없게 된다.
 */
@DisplayName("정책 부분 수정 (PATCH)")
class PolicyAdminServicePatchTest {

    private PolicyRepository policyRepository;
    private PolicyCategoryRepository policyCategoryRepository;
    private ObjectMapper objectMapper;
    private PolicyAdminService service;

    @BeforeEach
    void setUp() {
        policyRepository = mock(PolicyRepository.class);
        policyCategoryRepository = mock(PolicyCategoryRepository.class);
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        service = new PolicyAdminService(policyRepository, policyCategoryRepository, objectMapper);

        when(policyRepository.save(any(Policy.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("보내지 않은 필드는 그대로 둔다")
    void keepsAbsentFields() {
        Policy policy = existingPolicy();
        givenPolicy(policy);

        service.patch(1L, json("{\"title\":\"첫만남이용권(개정)\"}"));

        assertThat(policy.getTitle()).isEqualTo("첫만남이용권(개정)");
        // 전체 교체(PUT)였다면 아래가 전부 null 이 됐을 것이다.
        assertThat(policy.getPolicyCode()).isEqualTo("FIRST_MEETING");
        assertThat(policy.getBenefitAmount()).isEqualTo(2_000_000);
        assertThat(policy.getApplicationEndDate()).isEqualTo(LocalDate.of(2026, 12, 31));
        assertThat(policy.getPolicyType()).isEqualTo("VOUCHER");
        assertThat(policy.getPriority()).isEqualTo(5);
    }

    @Test
    @DisplayName("키가 있고 값이 null 이면 해당 항목을 비운다")
    void clearsExplicitNull() {
        Policy policy = existingPolicy();
        givenPolicy(policy);

        service.patch(1L, json("{\"applicationEndDate\":null,\"benefitAmount\":null}"));

        assertThat(policy.getApplicationEndDate()).isNull();
        assertThat(policy.getBenefitAmount()).isNull();
        // 함께 보내지 않은 값은 유지된다.
        assertThat(policy.getApplicationStartDate()).isEqualTo(LocalDate.of(2026, 1, 1));
    }

    @Test
    @DisplayName("날짜와 숫자를 형식에 맞게 변환한다")
    void convertsTypes() {
        Policy policy = existingPolicy();
        givenPolicy(policy);

        service.patch(1L, json("{\"applicationEndDate\":\"2027-03-31\",\"benefitAmount\":3000000}"));

        assertThat(policy.getApplicationEndDate()).isEqualTo(LocalDate.of(2027, 3, 31));
        assertThat(policy.getBenefitAmount()).isEqualTo(3_000_000);
    }

    @Test
    @DisplayName("필수 항목은 비울 수 없다")
    void rejectsBlankingRequiredFields() {
        givenPolicy(existingPolicy());

        assertThatThrownBy(() -> service.patch(1L, json("{\"title\":null}")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("정책명");

        assertThatThrownBy(() -> service.patch(1L, json("{\"policyCode\":\"  \"}")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("정책 코드");
    }

    @Test
    @DisplayName("다른 정책이 쓰는 코드로는 바꿀 수 없다")
    void rejectsDuplicatePolicyCode() {
        givenPolicy(existingPolicy());

        Policy other = new Policy();
        other.setId(2L);
        other.setPolicyCode("TAKEN");
        when(policyRepository.findByPolicyCode("TAKEN")).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> service.patch(1L, json("{\"policyCode\":\"TAKEN\"}")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("이미 존재하는");
    }

    @Test
    @DisplayName("자기 코드를 그대로 다시 보내는 것은 충돌이 아니다")
    void allowsSamePolicyCode() {
        Policy policy = existingPolicy();
        givenPolicy(policy);
        when(policyRepository.findByPolicyCode("FIRST_MEETING")).thenReturn(Optional.of(policy));

        service.patch(1L, json("{\"policyCode\":\"FIRST_MEETING\"}"));

        assertThat(policy.getPolicyCode()).isEqualTo("FIRST_MEETING");
    }

    @Test
    @DisplayName("노출 여부는 비우면 노출로 되돌린다")
    void neverLeavesIsActiveNull() {
        Policy policy = existingPolicy();
        givenPolicy(policy);

        service.patch(1L, json("{\"isActive\":null}"));

        // null 로 두면 목록 조회 조건에서 빠져 사라진 것처럼 보인다.
        assertThat(policy.getIsActive()).isTrue();
    }

    @Test
    @DisplayName("카테고리를 비우거나 바꿀 수 있다")
    void updatesCategory() {
        Policy policy = existingPolicy();
        givenPolicy(policy);

        // PolicyCategory 는 기본 생성자가 protected 라 테스트에서 직접 만들 수 없다.
        PolicyCategory category = mock(PolicyCategory.class);
        when(policyCategoryRepository.findById(7L)).thenReturn(Optional.of(category));

        service.patch(1L, json("{\"policyCategoryId\":7}"));
        assertThat(policy.getPolicyCategory()).isEqualTo(category);

        service.patch(1L, json("{\"policyCategoryId\":null}"));
        assertThat(policy.getPolicyCategory()).isNull();
    }

    @Test
    @DisplayName("없는 카테고리를 지정하면 거부한다")
    void rejectsUnknownCategory() {
        givenPolicy(existingPolicy());
        when(policyCategoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.patch(1L, json("{\"policyCategoryId\":99}")))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("빈 본문은 아무것도 바꾸지 않는다")
    void emptyBodyChangesNothing() {
        Policy policy = existingPolicy();
        givenPolicy(policy);

        AdminPolicyDetailResponse response = service.patch(1L, json("{}"));

        assertThat(response.getTitle()).isEqualTo("첫만남이용권");
        assertThat(policy.getBenefitAmount()).isEqualTo(2_000_000);
    }

    @Test
    @DisplayName("객체가 아닌 본문은 거부한다")
    void rejectsNonObjectBody() {
        givenPolicy(existingPolicy());

        assertThatThrownBy(() -> service.patch(1L, json("[]")))
                .isInstanceOf(BusinessException.class);
    }

    private void givenPolicy(Policy policy) {
        when(policyRepository.findById(1L)).thenReturn(Optional.of(policy));
    }

    private Policy existingPolicy() {
        Policy policy = new Policy();
        policy.setId(1L);
        policy.setPolicyCode("FIRST_MEETING");
        policy.setTitle("첫만남이용권");
        policy.setPolicyType("VOUCHER");
        policy.setBenefitAmount(2_000_000);
        policy.setBenefitType("LUMP_SUM");
        policy.setApplicationStartDate(LocalDate.of(2026, 1, 1));
        policy.setApplicationEndDate(LocalDate.of(2026, 12, 31));
        policy.setPriority(5);
        policy.setIsActive(true);
        return policy;
    }

    private JsonNode json(String raw) {
        try {
            return objectMapper.readTree(raw);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
