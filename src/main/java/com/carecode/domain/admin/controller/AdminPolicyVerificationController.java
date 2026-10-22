package com.carecode.domain.admin.controller;

import com.carecode.domain.admin.dto.response.PolicyVerificationResponse;
import com.carecode.domain.admin.dto.response.RegionVerificationStatusResponse;
import com.carecode.domain.admin.service.PolicyVerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 정책 금액 검증. 자동 수집된 금액을 사람이 확인한 뒤 확정으로 표시한다. */
@RestController
@RequestMapping("/api/admin/policies")
@RequiredArgsConstructor
@Tag(name = "어드민 - 정책 검증", description = "지원금 금액 수기 검증")
public class AdminPolicyVerificationController {

    private final PolicyVerificationService policyVerificationService;

    @PostMapping("/{policyId}/verify")
    @Operation(summary = "정책 금액 검증 표시", description = "확인한 금액을 확정으로 전환")
    public ResponseEntity<PolicyVerificationResponse> verify(
            @Parameter(description = "정책 ID", required = true) @PathVariable Long policyId,
            @Parameter(description = "금액 근거 출처 URL") @RequestParam(required = false) String sourceUrl) {
        return ResponseEntity.ok(policyVerificationService.verify(policyId, sourceUrl));
    }

    @DeleteMapping("/{policyId}/verify")
    @Operation(summary = "검증 표시 해제", description = "추정치로 되돌림")
    public ResponseEntity<Void> unverify(@PathVariable Long policyId) {
        policyVerificationService.unverify(policyId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/verification-status")
    @Operation(summary = "지역별 검증 현황", description = "미검증 정책이 많은 지역 순")
    public ResponseEntity<List<RegionVerificationStatusResponse>> status() {
        return ResponseEntity.ok(policyVerificationService.statusByRegion());
    }
}
