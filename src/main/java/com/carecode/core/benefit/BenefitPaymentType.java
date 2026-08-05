package com.carecode.core.benefit;

import java.util.List;

/** 지원금 지급 방식. 누적 수령액을 계산하려면 월 지급인지 일시금인지부터 갈라야 한다. */
public enum BenefitPaymentType {

    /** 대상 기간 동안 매월 지급. */
    MONTHLY,

    /** 조건 충족 시 1회 지급. */
    ONE_TIME,

    /** 현금이 아니라 서비스·할인·공급. 금액 합산에서 제외한다. */
    NON_CASH,

    /** 표기가 없거나 판별 불가. */
    UNKNOWN;

    private static final List<String> MONTHLY_MARKERS = List.of("월지급", "월지원", "월급여", "매월", "월 지급");
    private static final List<String> ONE_TIME_MARKERS = List.of("일시", "일회", "1회", "출산지원금", "축하금");
    private static final List<String> NON_CASH_MARKERS = List.of(
            "서비스", "무료", "할인", "공급", "감면", "면제", "이용권", "제공");

    /** 지급 방식 문자열에서 판별한다. 공공데이터 표기가 제각각이라 부분 일치로 본다. */
    public static BenefitPaymentType resolve(String benefitType) {
        if (benefitType == null || benefitType.isBlank()) {
            return UNKNOWN;
        }
        String normalized = benefitType.replaceAll("\\s+", "");

        if (NON_CASH_MARKERS.stream().anyMatch(normalized::contains)) {
            return NON_CASH;
        }
        if (MONTHLY_MARKERS.stream().anyMatch(m -> normalized.contains(m.replaceAll("\\s+", "")))) {
            return MONTHLY;
        }
        if (ONE_TIME_MARKERS.stream().anyMatch(normalized::contains)) {
            return ONE_TIME;
        }
        // "월" 단독 표기도 월 지급으로 본다.
        return normalized.startsWith("월") ? MONTHLY : UNKNOWN;
    }

    /** 금액 합산에 포함할지. UNKNOWN 은 포함하되 호출부에서 1회 지급으로 취급한다 — 월 지급으로 잘못 보면 60배까지 부풀려진다. */
    public boolean countsTowardCash() {
        return this != NON_CASH;
    }
}
