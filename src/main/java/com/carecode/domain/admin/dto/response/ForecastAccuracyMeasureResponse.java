package com.carecode.domain.admin.dto.response;

import com.carecode.domain.facility.entity.ForecastAccuracy;

import java.util.List;

/**
 * 예측 정확도 측정 실행 결과.
 *
 * <p>표본이 부족한 기간은 {@code results} 에 들어가지 않는다. 없는 정확도를 만들어내지 않기
 * 위해서다. 그래서 {@code measured} 가 요청한 기간 수보다 작을 수 있다.
 */
public record ForecastAccuracyMeasureResponse(int measured, List<ForecastAccuracyRow> results) {

    public static ForecastAccuracyMeasureResponse of(List<ForecastAccuracy> results) {
        return new ForecastAccuracyMeasureResponse(
                results.size(),
                results.stream().map(ForecastAccuracyRow::from).toList());
    }
}
