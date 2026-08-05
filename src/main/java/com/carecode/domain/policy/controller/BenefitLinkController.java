package com.carecode.domain.policy.controller;

import com.carecode.core.analytics.EventLogger;
import com.carecode.core.analytics.EventType;
import com.carecode.core.exception.CareServiceException;
import com.carecode.core.security.CurrentUserFacade;
import com.carecode.domain.policy.entity.Policy;
import com.carecode.domain.policy.repository.PolicyRepository;
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

    private final PolicyRepository policyRepository;
    private final EventLogger eventLogger;
    private final CurrentUserFacade currentUserFacade;

    @GetMapping("/{policyId}/apply")
    @Operation(summary = "지원금 신청 링크 이동", description = "클릭을 집계한 뒤 신청 페이지로 리다이렉트")
    public ResponseEntity<Void> apply(
            @Parameter(description = "정책 ID", required = true) @PathVariable Long policyId) {

        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new CareServiceException("정책을 찾을 수 없습니다: " + policyId));

        String url = policy.getApplicationUrl();
        if (url == null || url.isBlank()) {
            throw new CareServiceException("이 정책은 온라인 신청 경로가 없습니다.");
        }
        // 외부 URL 로 리다이렉트하므로 스킴을 확인한다. javascript: 같은 값이 들어오면 XSS 가 된다.
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            throw new CareServiceException("허용되지 않은 신청 링크입니다.");
        }

        eventLogger.log(EventType.BENEFIT_LINK_CLICKED, currentUserIdOrNull(), String.valueOf(policyId));

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
