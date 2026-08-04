package com.carecode.domain.policy.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/** 놓쳤을 가능성이 있는 지원금 한 건. */
@Getter
@Builder
public class MissedBenefitResponse {

    private Long policyId;
    private String title;
    private String childName;

    /** 해당 아이가 이 정책 대상이었던 구간(월령). */
    private Integer eligibleFromMonth;
    private Integer eligibleToMonth;

    /** 지금도 소급 신청이 가능한지. */
    private boolean claimable;

    /** 소급 신청 마감까지 남은 개월. claimable=false 면 null. */
    private Integer remainingMonths;

    /** 지원 금액(원). 확인되지 않으면 null. */
    private Integer benefitAmount;

    private String applicationUrl;

    /** 왜 이 정책이 목록에 올라왔는지. */
    private List<String> reasons;
}
