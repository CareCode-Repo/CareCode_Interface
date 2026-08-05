package com.carecode.domain.admin.controller;

import com.carecode.core.exception.CareServiceException;
import com.carecode.core.security.CurrentUserFacade;
import com.carecode.domain.policy.entity.Policy;
import com.carecode.domain.policy.repository.PolicyRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 정책 금액 검증. 자동 수집된 금액을 사람이 확인한 뒤 확정으로 표시한다. */
@Slf4j
@RestController
@RequestMapping("/api/admin/policies")
@RequiredArgsConstructor
@Tag(name = "어드민 - 정책 검증", description = "지원금 금액 수기 검증")
public class AdminPolicyVerificationController {

    private final PolicyRepository policyRepository;
    private final CurrentUserFacade currentUserFacade;

    @PostMapping("/{policyId}/verify")
    @Transactional
    @Operation(summary = "정책 금액 검증 표시", description = "확인한 금액을 확정으로 전환")
    public ResponseEntity<Map<String, Object>> verify(
            @Parameter(description = "정책 ID", required = true) @PathVariable Long policyId,
            @Parameter(description = "금액 근거 출처 URL") @RequestParam(required = false) String sourceUrl) {

        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new CareServiceException("정책을 찾을 수 없습니다: " + policyId));

        policy.setVerifiedAt(LocalDateTime.now());
        policy.setVerifiedBy(currentUserFacade.requireCurrentUserEmail());
        policy.setSourceUrl(sourceUrl);
        policyRepository.save(policy);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("policyId", policyId);
        body.put("title", policy.getTitle());
        body.put("verifiedAt", policy.getVerifiedAt());
        body.put("verifiedBy", policy.getVerifiedBy());
        return ResponseEntity.ok(body);
    }

    @DeleteMapping("/{policyId}/verify")
    @Transactional
    @Operation(summary = "검증 표시 해제", description = "추정치로 되돌림")
    public ResponseEntity<Void> unverify(@PathVariable Long policyId) {
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new CareServiceException("정책을 찾을 수 없습니다: " + policyId));
        policy.setVerifiedAt(null);
        policy.setVerifiedBy(null);
        policyRepository.save(policy);
        return ResponseEntity.noContent().build();
    }

    /** 검증 우선순위 판단용. 미검증 정책이 많은 지역부터 손봐야 한다. */
    @GetMapping("/verification-status")
    @Operation(summary = "지역별 검증 현황", description = "미검증 정책이 많은 지역 순")
    public ResponseEntity<List<Map<String, Object>>> status() {
        Map<String, List<Policy>> byRegion = policyRepository.findByIsActiveTrue().stream()
                .filter(p -> p.getTargetRegion() != null && !p.getTargetRegion().isBlank())
                .collect(Collectors.groupingBy(Policy::getTargetRegion));

        List<Map<String, Object>> rows = byRegion.entrySet().stream().map(e -> {
            long verified = e.getValue().stream().filter(p -> p.getVerifiedAt() != null).count();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("region", e.getKey());
            row.put("total", e.getValue().size());
            row.put("verified", verified);
            row.put("unverified", e.getValue().size() - verified);
            row.put("verifiedRate", e.getValue().isEmpty() ? 0
                    : (int) Math.round(100.0 * verified / e.getValue().size()));
            return row;
        }).sorted(Comparator.comparingInt(r -> (Integer) r.get("verifiedRate"))).toList();

        return ResponseEntity.ok(rows);
    }
}
