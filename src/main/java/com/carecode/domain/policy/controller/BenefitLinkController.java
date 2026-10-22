package com.carecode.domain.policy.controller;

import com.carecode.core.security.CurrentUserFacade;
import com.carecode.domain.policy.service.BenefitLinkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * 지원금 신청 링크를 거쳐 가게 해서 클릭을 집계한다.
 * 이 전환율이 서비스가 실제로 돈을 찾아줬는지 증명하는 유일한 지표다.
 */
@Slf4j
@RestController
@RequestMapping("/policies")
@RequiredArgsConstructor
@Tag(name = "육아 정책", description = "육아 정책 정보 및 검색 API")
public class BenefitLinkController {

    private final BenefitLinkService benefitLinkService;
    private final CurrentUserFacade currentUserFacade;

    @GetMapping("/{policyId}/apply")
    @Operation(summary = "지원금 신청 링크 이동", description = "클릭을 집계한 뒤 신청 페이지로 리다이렉트")
    public ResponseEntity<Void> apply(
            @Parameter(description = "정책 ID", required = true) @PathVariable Long policyId) {

        String url = benefitLinkService.resolveApplyUrl(policyId, currentUserIdOrNull());

        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(url)).build();
    }

    /** 비로그인 클릭도 집계 대상이다. 인증 실패로 리다이렉트를 막지 않는다. */
    private Long currentUserIdOrNull() {
        try {
            return currentUserFacade.requireCurrentUserDbId();
        } catch (Exception e) {
            return null;
        }
    }
}
