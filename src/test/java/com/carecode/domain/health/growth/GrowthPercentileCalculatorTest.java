package com.carecode.domain.health.growth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/** WHO LMS 백분위 계산 검증. 기존 차트 API 는 상수(DEFAULT_NUTRITION_PROGRESS)를 반환해 또래 비교가 불가능했다. */
@DisplayName("성장 백분위 계산")
class GrowthPercentileCalculatorTest {

    @Test
    @DisplayName("중앙값을 입력하면 50 백분위가 나온다")
    void medianValueYieldsFiftiethPercentile() {
        // 남아 12개월 체중 중앙값 M = 9.6479kg
        Optional<GrowthPercentileResult> result =
                GrowthPercentileCalculator.calculate(GrowthMetric.WEIGHT, Sex.MALE, 12, 9.6479);

        assertThat(result).isPresent();
        assertThat(result.get().percentile()).isCloseTo(50.0, within(1.0));
        assertThat(result.get().zScore()).isCloseTo(0.0, within(0.05));
        assertThat(result.get().needsAttention()).isFalse();
    }

    @Test
    @DisplayName("중앙값보다 크면 50 백분위를 넘는다")
    void aboveMedianYieldsHigherPercentile() {
        Optional<GrowthPercentileResult> result =
                GrowthPercentileCalculator.calculate(GrowthMetric.WEIGHT, Sex.MALE, 12, 11.5);

        assertThat(result).isPresent();
        assertThat(result.get().percentile()).isGreaterThan(50.0);
        assertThat(result.get().zScore()).isPositive();
    }

    @Test
    @DisplayName("중앙값보다 작으면 50 백분위 미만이다")
    void belowMedianYieldsLowerPercentile() {
        Optional<GrowthPercentileResult> result =
                GrowthPercentileCalculator.calculate(GrowthMetric.WEIGHT, Sex.MALE, 12, 8.0);

        assertThat(result).isPresent();
        assertThat(result.get().percentile()).isLessThan(50.0);
        assertThat(result.get().zScore()).isNegative();
    }

    @Test
    @DisplayName("표에 없는 개월 수는 선형 보간한다")
    void interpolatesBetweenTableEntries() {
        // 9개월은 표(6, 12개월)에 없으므로 보간된다
        Optional<GrowthPercentileResult> result =
                GrowthPercentileCalculator.calculate(GrowthMetric.HEIGHT, Sex.FEMALE, 9, 72.0);

        assertThat(result).isPresent();
        // 6개월(65.73) ~ 12개월(74.02) 사이의 중앙값이어야 한다
        assertThat(result.get().medianValue()).isBetween(65.7, 74.1);
    }

    @Test
    @DisplayName("극단값은 주의 구간으로 표시한다")
    void flagsExtremeValues() {
        Optional<GrowthPercentileResult> result =
                GrowthPercentileCalculator.calculate(GrowthMetric.WEIGHT, Sex.MALE, 12, 5.5);

        assertThat(result).isPresent();
        assertThat(result.get().needsAttention()).isTrue();
        assertThat(result.get().interpretation()).contains("낮음");
    }

    @Test
    @DisplayName("WHO 표준 적용 범위(0~60개월)를 벗어나면 계산하지 않는다")
    void returnsEmptyOutsideStandardRange() {
        assertThat(GrowthPercentileCalculator.calculate(GrowthMetric.WEIGHT, Sex.MALE, 72, 20.0))
                .isEmpty();
    }

    @Test
    @DisplayName("0 이하 측정값은 계산하지 않는다")
    void returnsEmptyForNonPositiveValue() {
        assertThat(GrowthPercentileCalculator.calculate(GrowthMetric.WEIGHT, Sex.MALE, 12, 0))
                .isEmpty();
    }

    @Test
    @DisplayName("성별 표기가 달라도 파싱된다")
    void parsesVariousSexNotations() {
        assertThat(Sex.parse("남")).contains(Sex.MALE);
        assertThat(Sex.parse("MALE")).contains(Sex.MALE);
        assertThat(Sex.parse("여아")).contains(Sex.FEMALE);
        assertThat(Sex.parse("f")).contains(Sex.FEMALE);
        assertThat(Sex.parse("unknown")).isEmpty();
        assertThat(Sex.parse(null)).isEmpty();
    }
}
