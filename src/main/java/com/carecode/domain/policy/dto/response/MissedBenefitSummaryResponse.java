package com.carecode.domain.policy.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/** 놓친 지원금 요약. 첫 화면에 "놓친 금액" 을 띄우기 위한 응답이다. */
@Getter
@Builder
public class MissedBenefitSummaryResponse {

    /** 아직 소급 신청이 가능한 건수. */
    private int claimableCount;

    /** 소급 가능한 건의 지원금 합계(원). 금액 미상 정책은 제외된다. */
    private long claimableAmount;

    /** 기간이 지나 신청할 수 없게 된 건수. 같은 실수를 반복하지 않도록 함께 보여준다. */
    private int expiredCount;

    /** 소득 정보가 없어 판정을 보류한 건수. */
    private int unknownEligibilityCount;

    private List<MissedBenefitResponse> claimable;
    private List<MissedBenefitResponse> expired;
}
