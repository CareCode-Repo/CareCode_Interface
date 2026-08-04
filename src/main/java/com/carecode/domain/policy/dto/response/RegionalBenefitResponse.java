package com.carecode.domain.policy.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/** 지역 한 곳의 예상 수령액. */
@Getter
@Builder
public class RegionalBenefitResponse {

    private String region;

    /** 전국 정책 + 해당 지역 정책의 기간 내 예상 총액(원). */
    private long totalAmount;

    /** 현재 거주지 대비 차액(원). 음수면 지금이 더 유리하다. */
    private long differenceFromBase;

    /** 금액으로 환산된 정책 수. */
    private int cashPolicyCount;

    /** 금액이 아닌 혜택(무료검진·서비스 등) 수. 합산에는 빠져 있다. */
    private int nonCashPolicyCount;

    /** 금액 상위 기여 정책. 왜 이 지역이 높은지 설명한다. */
    private List<Contribution> topContributors;

    @Getter
    @Builder
    public static class Contribution {
        private String title;
        private long amount;
        private String paymentType;
    }
}
