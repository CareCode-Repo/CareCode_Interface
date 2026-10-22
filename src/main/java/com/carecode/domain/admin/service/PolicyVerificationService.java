package com.carecode.domain.admin.service;

import com.carecode.core.exception.ResourceNotFoundException;
import com.carecode.domain.user.app.CurrentUserFacade;
import com.carecode.domain.admin.dto.response.PolicyVerificationResponse;
import com.carecode.domain.admin.dto.response.RegionVerificationStatusResponse;
import com.carecode.domain.policy.entity.Policy;
import com.carecode.domain.policy.repository.PolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 정책 금액 검증. 자동 수집된 금액을 사람이 확인한 뒤 확정으로 표시한다.
 *
 * <p>금액이 "추정치" 에서 "확정" 으로 바뀌는 지점이라 누가 언제 확인했는지를 함께 남긴다.
 * 근거 출처를 남기지 않으면 나중에 금액이 달라졌을 때 무엇을 보고 확정했는지 알 수 없다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PolicyVerificationService {

    private final PolicyRepository policyRepository;
    private final CurrentUserFacade currentUserFacade;

    @Transactional
    public PolicyVerificationResponse verify(Long policyId, String sourceUrl) {
        Policy policy = require(policyId);

        policy.setVerifiedAt(LocalDateTime.now());
        policy.setVerifiedBy(currentUserFacade.requireCurrentUserEmail());
        policy.setSourceUrl(sourceUrl);
        policyRepository.save(policy);

        return PolicyVerificationResponse.from(policy);
    }

    @Transactional
    public void unverify(Long policyId) {
        Policy policy = require(policyId);
        policy.setVerifiedAt(null);
        policy.setVerifiedBy(null);
        policyRepository.save(policy);
    }

    /** 검증 우선순위 판단용. 미검증 정책이 많은 지역부터 손봐야 한다. */
    public List<RegionVerificationStatusResponse> statusByRegion() {
        Map<String, List<Policy>> byRegion = policyRepository.findByIsActiveTrue().stream()
                .filter(p -> p.getTargetRegion() != null && !p.getTargetRegion().isBlank())
                .collect(Collectors.groupingBy(Policy::getTargetRegion));

        return byRegion.entrySet().stream()
                .map(e -> RegionVerificationStatusResponse.of(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingInt(RegionVerificationStatusResponse::verifiedRate))
                .toList();
    }

    private Policy require(Long policyId) {
        return policyRepository.findById(policyId)
                .orElseThrow(() -> new ResourceNotFoundException("정책을 찾을 수 없습니다: " + policyId));
    }
}
