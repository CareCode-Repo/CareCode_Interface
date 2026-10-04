package com.carecode.domain.admin.dto.response;

import com.carecode.domain.policy.entity.Policy;

import java.util.List;

/**
 * 지역별 정책 금액 검증 현황. 검증률이 낮은 지역부터 손봐야 한다.
 *
 * <p>{@code verifiedRate} 는 백분율 정수다. 표본이 작을 때 소수점을 보여 주면 1건 차이가
 * 큰 변화처럼 읽힌다.
 */
public record RegionVerificationStatusResponse(
        String region,
        int total,
        long verified,
        long unverified,
        int verifiedRate) {

    public static RegionVerificationStatusResponse of(String region, List<Policy> policies) {
        long verified = policies.stream().filter(p -> p.getVerifiedAt() != null).count();
        int rate = policies.isEmpty() ? 0 : (int) Math.round(100.0 * verified / policies.size());
        return new RegionVerificationStatusResponse(
                region, policies.size(), verified, policies.size() - verified, rate);
    }
}
