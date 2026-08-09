package com.carecode.domain.admin.service;

import com.carecode.core.exception.BusinessException;
import com.carecode.core.exception.ErrorCode;
import com.carecode.core.exception.PolicyNotFoundException;
import com.carecode.domain.admin.dto.AdminPolicyDetailResponse;
import com.carecode.domain.admin.dto.AdminPolicyPatchRequest;
import com.carecode.domain.admin.dto.AdminPolicyRequest;
import com.carecode.domain.policy.entity.Policy;
import com.carecode.domain.policy.entity.PolicyCategory;
import com.carecode.domain.policy.repository.PolicyCategoryRepository;
import com.carecode.domain.policy.repository.PolicyRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/** 관리자 정책 관리. */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PolicyAdminService {

    private final PolicyRepository policyRepository;
    private final PolicyCategoryRepository policyCategoryRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public AdminPolicyDetailResponse create(AdminPolicyRequest request) {
        assertPolicyCodeAvailable(request.getPolicyCode(), null);

        Policy policy = new Policy();
        apply(policy, request);
        policy.setViewCount(0);

        Policy saved = policyRepository.save(policy);
        log.info("정책 생성 - policyId={}, code={}", saved.getId(), saved.getPolicyCode());
        return AdminPolicyDetailResponse.from(saved);
    }

    /** 정책 전체 교체. 요청에 없는 필드는 null 이 되므로 모든 값을 담아 보내야 한다. */
    @Transactional
    @CacheEvict(cacheNames = "policy", key = "#policyId")
    public AdminPolicyDetailResponse update(Long policyId, AdminPolicyRequest request) {
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new PolicyNotFoundException("정책을 찾을 수 없습니다: " + policyId));

        apply(policy, request);
        return AdminPolicyDetailResponse.from(policyRepository.save(policy));
    }

    /**
     * 정책 부분 수정.
     *
     * <p>요청 JSON 에 담긴 키만 반영한다. 키가 없으면 기존 값을 그대로 두고, 키가 있는데 값이
     * null 이면 비운다. 값의 null 여부만 보면 "비우기" 와 "건드리지 않기" 를 구분할 수 없다.
     *
     * @param body 원본 요청 JSON. 어떤 키가 왔는지 확인하기 위해 트리 형태로 받는다.
     */
    @Transactional
    @CacheEvict(cacheNames = "policy", key = "#policyId")
    public AdminPolicyDetailResponse patch(Long policyId, JsonNode body) {
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new PolicyNotFoundException("정책을 찾을 수 없습니다: " + policyId));

        AdminPolicyPatchRequest request = toPatchRequest(body);

        applyIfPresent(body, "policyCode", () -> {
            requireText(request.getPolicyCode(), "정책 코드");
            assertPolicyCodeAvailable(request.getPolicyCode(), policyId);
            policy.setPolicyCode(request.getPolicyCode());
        });
        applyIfPresent(body, "title", () -> {
            requireText(request.getTitle(), "정책명");
            policy.setTitle(request.getTitle());
        });
        applyIfPresent(body, "description", () -> policy.setDescription(request.getDescription()));
        applyIfPresent(body, "policyType", () -> policy.setPolicyType(request.getPolicyType()));
        applyIfPresent(body, "targetAgeMin", () -> policy.setTargetAgeMin(request.getTargetAgeMin()));
        applyIfPresent(body, "targetAgeMax", () -> policy.setTargetAgeMax(request.getTargetAgeMax()));
        applyIfPresent(body, "targetRegion", () -> policy.setTargetRegion(request.getTargetRegion()));
        applyIfPresent(body, "benefitAmount", () -> policy.setBenefitAmount(request.getBenefitAmount()));
        applyIfPresent(body, "benefitType", () -> policy.setBenefitType(request.getBenefitType()));
        applyIfPresent(body, "applicationStartDate",
                () -> policy.setApplicationStartDate(request.getApplicationStartDate()));
        applyIfPresent(body, "applicationEndDate",
                () -> policy.setApplicationEndDate(request.getApplicationEndDate()));
        applyIfPresent(body, "policyStartDate",
                () -> policy.setPolicyStartDate(request.getPolicyStartDate()));
        applyIfPresent(body, "policyEndDate", () -> policy.setPolicyEndDate(request.getPolicyEndDate()));
        applyIfPresent(body, "applicationUrl",
                () -> policy.setApplicationUrl(request.getApplicationUrl()));
        applyIfPresent(body, "contactInfo", () -> policy.setContactInfo(request.getContactInfo()));
        applyIfPresent(body, "requiredDocuments",
                () -> policy.setRequiredDocuments(request.getRequiredDocuments()));
        applyIfPresent(body, "priority", () -> policy.setPriority(request.getPriority()));

        // 노출 여부는 비울 수 없다. null 로 두면 목록 조회 조건에서 빠져 사라진 것처럼 보인다.
        applyIfPresent(body, "isActive", () -> policy.setIsActive(
                request.getIsActive() != null ? request.getIsActive() : Boolean.TRUE));

        applyIfPresent(body, "policyCategoryId", () -> policy.setPolicyCategory(
                request.getPolicyCategoryId() != null
                        ? findCategory(request.getPolicyCategoryId())
                        : null));

        log.info("정책 부분 수정 - policyId={}, 변경 필드={}", policyId, fieldNames(body));
        return AdminPolicyDetailResponse.from(policyRepository.save(policy));
    }

    private AdminPolicyPatchRequest toPatchRequest(JsonNode body) {
        if (body == null || !body.isObject()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "요청 본문이 올바르지 않습니다.");
        }
        try {
            return objectMapper.treeToValue(body, AdminPolicyPatchRequest.class);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "요청 값을 해석할 수 없습니다: " + e.getOriginalMessage());
        }
    }

    private void applyIfPresent(JsonNode body, String field, Runnable apply) {
        if (body.has(field)) {
            apply.run();
        }
    }

    /** 부분 수정이라도 필수 항목을 빈 값으로 만들 수는 없다. */
    private void requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, label + "은(는) 비울 수 없습니다.");
        }
    }

    /** @param policyId 수정 중인 정책. 신규 등록이면 null (자기 자신과의 충돌 검사가 없다) */
    private void assertPolicyCodeAvailable(String policyCode, Long policyId) {
        policyRepository.findByPolicyCode(policyCode)
                .filter(existing -> !existing.getId().equals(policyId))
                .ifPresent(existing -> {
                    throw new BusinessException(ErrorCode.INVALID_INPUT,
                            "이미 존재하는 정책 코드입니다: " + policyCode);
                });
    }

    private PolicyCategory findCategory(Long categoryId) {
        return policyCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POLICY_CATEGORY_NOT_FOUND,
                        "정책 카테고리를 찾을 수 없습니다: " + categoryId));
    }

    private String fieldNames(JsonNode body) {
        List<String> names = new ArrayList<>();
        body.fieldNames().forEachRemaining(names::add);
        return String.join(", ", names);
    }

    @Transactional
    @CacheEvict(cacheNames = "policy", key = "#policyId")
    public void delete(Long policyId) {
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new PolicyNotFoundException("정책을 찾을 수 없습니다: " + policyId));
        policyRepository.delete(policy);
    }

    private void apply(Policy policy, AdminPolicyRequest request) {
        policy.setPolicyCode(request.getPolicyCode());
        policy.setTitle(request.getTitle());
        policy.setDescription(request.getDescription());
        policy.setPolicyType(request.getPolicyType());
        policy.setTargetAgeMin(request.getTargetAgeMin());
        policy.setTargetAgeMax(request.getTargetAgeMax());
        policy.setTargetRegion(request.getTargetRegion());
        policy.setBenefitAmount(request.getBenefitAmount());
        policy.setBenefitType(request.getBenefitType());
        policy.setApplicationStartDate(request.getApplicationStartDate());
        policy.setApplicationEndDate(request.getApplicationEndDate());
        policy.setPolicyStartDate(request.getPolicyStartDate());
        policy.setPolicyEndDate(request.getPolicyEndDate());
        policy.setApplicationUrl(request.getApplicationUrl());
        policy.setContactInfo(request.getContactInfo());
        policy.setRequiredDocuments(request.getRequiredDocuments());
        policy.setIsActive(request.getIsActive() != null ? request.getIsActive() : Boolean.TRUE);
        policy.setPriority(request.getPriority());

        if (request.getPolicyCategoryId() != null) {
            policy.setPolicyCategory(findCategory(request.getPolicyCategoryId()));
        }
    }
}
