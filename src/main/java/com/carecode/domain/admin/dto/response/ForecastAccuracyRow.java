package com.carecode.domain.admin.dto.response;

import com.carecode.domain.facility.entity.ForecastAccuracy;

/**
 * 예측 기간별 정확도 한 줄.
 *
 * <p>{@code brierScore} 는 낮을수록 좋고, 혼자서는 좋은지 알 수 없다. 그래서 "항상 평균 확률을
 * 답한다" 는 기준선({@code baselineBrierScore})을 함께 준다. {@code betterThanBaseline} 이 false 면
 * 예측이 기준선보다 못하다는 뜻이고, 그때는 예측을 보여 주지 않는 쪽이 맞다.
 */
public record ForecastAccuracyRow(
        int horizonMonths,
        int samples,
        int facilities,
        double actualRate,
        double brierScore,
        double baselineBrierScore,
        boolean betterThanBaseline) {

    public static ForecastAccuracyRow from(ForecastAccuracy accuracy) {
        return new ForecastAccuracyRow(
                accuracy.getHorizonMonths(),
                accuracy.getSamples(),
                accuracy.getFacilities(),
                accuracy.getActualRate(),
                accuracy.getBrierScore(),
                accuracy.getBaselineBrierScore(),
                accuracy.betterThanBaseline());
    }
}
