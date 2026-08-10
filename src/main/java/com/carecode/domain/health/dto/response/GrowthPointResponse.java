package com.carecode.domain.health.dto.response;

import com.carecode.domain.health.growth.GrowthMetric;
import com.carecode.domain.health.growth.GrowthPercentileResult;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

/** 성장 곡선의 한 지점. 백분위는 성별/생년월일이 없거나 WHO 표준 적용 범위(0~60개월)를 벗어나면 null 이다. */
@Getter
@Builder
public class GrowthPointResponse {

    private final LocalDate recordDate;
    private final int ageMonths;
    private final double value;
    private final String metric;
    private final String unit;

    private final Double percentile;
    private final Double zScore;
    private final Double medianValue;
    private final String interpretation;
    private final Boolean needsAttention;

    public static GrowthPointResponse of(LocalDate recordDate,
                                         int ageMonths,
                                         double value,
                                         GrowthMetric metric,
                                         GrowthPercentileResult percentileResult) {
        GrowthPointResponseBuilder builder = GrowthPointResponse.builder()
                .recordDate(recordDate)
                .ageMonths(ageMonths)
                .value(value)
                .metric(metric.name())
                .unit(metric.getUnit());

        if (percentileResult != null) {
            builder.percentile(percentileResult.percentile())
                    .zScore(percentileResult.zScore())
                    .medianValue(percentileResult.medianValue())
                    .interpretation(percentileResult.interpretation())
                    .needsAttention(percentileResult.needsAttention());
        }

        return builder.build();
    }
}
