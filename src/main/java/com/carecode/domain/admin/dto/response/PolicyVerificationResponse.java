package com.carecode.domain.admin.dto.response;

import com.carecode.domain.policy.entity.Policy;

import java.time.LocalDateTime;

/**
 * 정책 금액 검증 표시 결과.
 *
 * <p>누가 언제 확인했는지를 돌려준다. 금액이 "확정" 으로 보이기 시작하는 지점이므로, 근거를
 * 남긴 사람을 화면에서 바로 확인할 수 있어야 한다.
 */
public record PolicyVerificationResponse(
        Long policyId,
        String title,
        LocalDateTime verifiedAt,
        String verifiedBy) {

    public static PolicyVerificationResponse from(Policy policy) {
        return new PolicyVerificationResponse(
                policy.getId(), policy.getTitle(), policy.getVerifiedAt(), policy.getVerifiedBy());
    }
}
