package com.carecode.domain.policy.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/** 거주지별 지원금 비교 결과. */
@Getter
@Builder
public class RegionalBenefitComparisonResponse {

    private String childName;
    private Integer childAgeMonths;

    /** 전망 기간(개월). */
    private int horizonMonths;

    /** 기준이 된 현재 거주 지역. 주소 미입력이면 null. */
    private String baseRegion;
    private Long baseAmount;

    /** 총액 내림차순. */
    private List<RegionalBenefitResponse> rankings;

    /**
     * 데이터 신뢰 수준. 지자체 정책 수집이 불완전하면 실제와 차이가 날 수 있어 함께 노출한다.
     * VERIFIED / ESTIMATED
     */
    private String dataQuality;

    private List<String> disclaimers;
}
