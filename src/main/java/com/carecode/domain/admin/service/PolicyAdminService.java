package com.carecode.domain.admin.service;

import com.carecode.core.exception.BusinessException;
import com.carecode.core.exception.ErrorCode;
import com.carecode.core.exception.PolicyNotFoundException;
import com.carecode.domain.admin.dto.AdminPolicyRequest;
import com.carecode.domain.policy.dto.response.PolicyDto;
import com.carecode.domain.policy.entity.Policy;
import com.carecode.domain.policy.entity.PolicyCategory;
import com.carecode.domain.policy.mapper.PolicyMapper;
import com.carecode.domain.policy.repository.PolicyCategoryRepository;
import com.carecode.domain.policy.repository.PolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 관리자 정책 관리. */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PolicyAdminService {

    private final PolicyRepository policyRepository;
    private final PolicyCategoryRepository policyCategoryRepository;
    private final PolicyMapper policyMapper;

    @Transactional
    public PolicyDto create(AdminPolicyRequest request) {
        policyRepository.findByPolicyCode(request.getPolicyCode()).ifPresent(existing -> {
            throw new BusinessException(ErrorCode.INVALID_INPUT,
                    "이미 존재하는 정책 코드입니다: " + request.getPolicyCode());
        });

        Policy policy = new Policy();
        apply(policy, request);
        policy.setViewCount(0);

        Policy saved = policyRepository.save(policy);
        log.info("정책 생성 - policyId={}, code={}", saved.getId(), saved.getPolicyCode());
        return policyMapper.toResponse(saved);
    }

    /** 정책 수정. 캐시된 상세 응답이 낡지 않도록 해당 항목을 무효화한다. */
    @Transactional
    @CacheEvict(cacheNames = "policy", key = "#policyId")
    public PolicyDto update(Long policyId, AdminPolicyRequest request) {
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new PolicyNotFoundException("정책을 찾을 수 없습니다: " + policyId));

        apply(policy, request);
        return policyMapper.toResponse(policyRepository.save(policy));
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
            PolicyCategory category = policyCategoryRepository.findById(request.getPolicyCategoryId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.POLICY_CATEGORY_NOT_FOUND,
                            "정책 카테고리를 찾을 수 없습니다: " + request.getPolicyCategoryId()));
            policy.setPolicyCategory(category);
        }
    }
}
