package com.carecode.domain.policy.app;

import com.carecode.domain.policy.dto.request.PolicyRequest;
import com.carecode.domain.policy.dto.request.PolicySearchRequest;
import com.carecode.domain.policy.dto.response.PolicyResponse;
import com.carecode.domain.policy.dto.PolicyDto;
import com.carecode.domain.policy.dto.response.PolicyListResponse;
import com.carecode.domain.policy.dto.response.PolicyStatsSimpleResponse;
import com.carecode.domain.policy.service.PolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PolicyFacade {

    private final PolicyService policyService;

    @Transactional(readOnly = true)
    public List<PolicyDto> getAllPolicies() { return policyService.getAllPolicies(); }

    @Transactional(readOnly = true)
    public PolicyDto getPolicyById(Long policyId) { return policyService.getPolicyById(policyId); }

    @Transactional(readOnly = true)
    public PolicyListResponse searchPolicies(PolicySearchRequest request) { return policyService.searchPolicies(request); }

    @Transactional(readOnly = true)
    public List<PolicyDto> getPoliciesByCategory(String category) { return policyService.getPoliciesByCategory(category); }

    @Transactional(readOnly = true)
    public List<PolicyDto> getPoliciesByLocation(String location) { return policyService.getPoliciesByLocation(location); }

    @Transactional(readOnly = true)
    public List<PolicyDto> getPoliciesByAgeRange(Integer minAge, Integer maxAge) { return policyService.getPoliciesByAgeRange(minAge, maxAge); }

    @Transactional(readOnly = true)
    public List<PolicyDto> getPopularPolicies(Integer limit) { return policyService.getPopularPolicies(limit); }

    @Transactional(readOnly = true)
    public List<PolicyDto> getLatestPolicies(Integer limit) { return policyService.getLatestPolicies(limit); }

    @Transactional
    public void incrementViewCount(Long policyId) { policyService.incrementViewCount(policyId); }

    @Transactional(readOnly = true)
    public List<String> getPolicyCategories() { return policyService.getPolicyCategories(); }

    @Transactional(readOnly = true)
    public PolicyStatsSimpleResponse getPolicyStats() { return policyService.getPolicyStats(); }
}


