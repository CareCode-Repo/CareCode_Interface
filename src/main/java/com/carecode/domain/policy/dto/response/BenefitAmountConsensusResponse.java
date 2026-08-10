package com.carecode.domain.policy.dto.response;

import lombok.Builder;
import lombok.Getter;

/** 제보 합의 현황. 몇 명이 더 필요한지 보여줘야 참여가 이어진다. */
@Getter
@Builder
public class BenefitAmountConsensusResponse {

    private Long policyId;
    private String title;

    /** 이 정책에 들어온 제보 수. */
    private long totalReports;

    /** 확정에 필요한 동일 응답 수. */
    private int consensusThreshold;

    /** 가장 많이 나온 값에 동의한 사람 수. */
    private long agreedCount;

    private Integer consensusAmount;
    private String consensusPaymentType;

    /** 확정 여부. true 면 정책 금액이 채워졌다. */
    private boolean confirmed;

    /** 현재 정책에 저장된 금액. 확정 전이면 null 일 수 있다. */
    private Integer currentAmount;

    /** 확정까지 남은 제보 수. */
    public long getRemainingForConsensus() {
        return Math.max(0, consensusThreshold - agreedCount);
    }
}
