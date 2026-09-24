package com.carecode.domain.careFacility.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

/**
 * 입소 예측이 실제로 얼마나 맞았는지.
 *
 * <p>확률만 보여주면 사용자는 그 숫자를 믿을지 판단할 근거가 없다. 과거 관측으로 같은 계산을 다시 돌려
 * 측정한 적중률을 함께 준다. 표본이 적으면 숫자를 만들지 않고 비운다.
 */
@Getter
@Builder
public class ForecastAccuracyResponse {

    /** 측정 실행일. */
    private final LocalDate measuredAt;

    private final int horizonMonths;

    /** 검증에 쓴 예측 건수. */
    private final int samples;

    private final int facilities;

    /** 표본에서 실제로 자리가 난 비율 (0~1). */
    private final double actualRate;

    /** 낮을수록 정확. 0=완벽, 0.25=동전 던지기. */
    private final double brierScore;

    /** 항상 평균 발생률로 답했을 때의 점수. */
    private final double baselineBrierScore;

    /** 예측이 "평균으로 답하기" 보다 나은가. false 면 화면에서 확률을 강조하지 않는 편이 맞다. */
    private final boolean betterThanBaseline;

    /** 지금 보여주는 확률이 속한 구간의 실제 적중률. 그 구간 표본이 적으면 null. */
    private final Bucket matchedBucket;

    /** 전체 구간표. 공개 통계에서 쓴다. 상세 응답에서는 생략될 수 있다. */
    private final List<Bucket> calibration;

    @Getter
    @Builder
    public static class Bucket {
        /** 구간 시작(%)·끝(%). 예: 60~80 */
        private final int from;
        private final int to;
        private final int samples;
        private final int actualTrue;
        /** 이 구간으로 예측한 건 중 실제로 자리가 난 비율 (0~1). 표본 0이면 null. */
        private final Double actualRate;
    }
}
