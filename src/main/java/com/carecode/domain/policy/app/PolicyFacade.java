package com.carecode.domain.policy.app;

import com.carecode.domain.policy.dto.request.PolicySearchRequest;
import com.carecode.domain.policy.dto.request.BenefitAmountReportRequest;
import com.carecode.domain.policy.dto.response.BenefitAmountConsensusResponse;
import com.carecode.domain.policy.dto.response.MissedBenefitSummaryResponse;
import com.carecode.domain.policy.dto.response.PersonalizedPolicyResponse;
import com.carecode.domain.policy.dto.response.RegionalBenefitComparisonResponse;
import com.carecode.domain.policy.dto.response.PolicyBookmarkResponse;
import com.carecode.domain.policy.dto.response.PolicyDto;
import com.carecode.domain.policy.dto.response.PolicyListResponse;
import com.carecode.domain.policy.dto.response.PolicyStatsSimpleResponse;
import com.carecode.domain.policy.service.BenefitAmountReportService;
import com.carecode.domain.policy.service.MissedBenefitService;
import com.carecode.domain.policy.service.PolicyRecommendationService;
import com.carecode.domain.policy.service.RegionalBenefitComparisonService;
import com.carecode.domain.policy.service.PolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PolicyFacade {

    private final PolicyService policyService;
    private final PolicyRecommendationService policyRecommendationService;
    private final MissedBenefitService missedBenefitService;
    private final BenefitAmountReportService benefitAmountReportService;
    private final RegionalBenefitComparisonService regionalBenefitComparisonService;

    @Transactional(readOnly = true)
    public List<PolicyDto> getAllPolicies(int page, int size) { return policyService.getAllPolicies(page, size); }

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

    @Transactional(readOnly = true)
    public List<PolicyDto> getPoliciesByChildAge(Integer childAge) { return policyService.getPoliciesByChildAge(childAge); }

    @Transactional(readOnly = true)
    public List<PolicyDto> getActivePoliciesByDate() { return policyService.getActivePoliciesByDate(); }

    @Transactional
    public PolicyBookmarkResponse addBookmark(String userIdOrEmail, Long policyId) {
        return policyService.addBookmark(userIdOrEmail, policyId);
    }

    @Transactional(readOnly = true)
    public List<PolicyBookmarkResponse> getBookmarks(String userIdOrEmail) {
        return policyService.getBookmarks(userIdOrEmail);
    }

    @Transactional
    public void removeBookmark(String userIdOrEmail, Long policyId) {
        policyService.removeBookmark(userIdOrEmail, policyId);
    }

    @Transactional(readOnly = true)
    public List<PersonalizedPolicyResponse> recommendPolicies(int limit) {
        return policyRecommendationService.recommendForCurrentUser(limit);
    }

    @Transactional(readOnly = true)
    public MissedBenefitSummaryResponse findMissedBenefits() {
        return missedBenefitService.findMissedBenefits();
    }

    @Transactional(readOnly = true)
    public RegionalBenefitComparisonResponse compareRegionalBenefits(Long childId, Integer years, Integer limit) {
        return regionalBenefitComparisonService.compare(childId, years, limit);
    }

    @Transactional
    public BenefitAmountConsensusResponse reportBenefitAmount(Long policyId,
                                                              BenefitAmountReportRequest request) {
        return benefitAmountReportService.report(policyId, request);
    }

    @Transactional(readOnly = true)
    public BenefitAmountConsensusResponse getBenefitAmountConsensus(Long policyId) {
        return benefitAmountReportService.getConsensus(policyId);
    }
}
