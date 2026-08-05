package com.carecode.domain.health.growth;

import java.util.Optional;

/** WHO LMS 방식 백분위 계산기. Z-score = ((측정값 / M)^L - 1) / (L × S), L=0 이면 ln(측정값/M) / S */
public final class GrowthPercentileCalculator {

    private GrowthPercentileCalculator() {
    }

    public static Optional<GrowthPercentileResult> calculate(GrowthMetric metric,
                                                             Sex sex,
                                                             int ageMonths,
                                                             double measuredValue) {
        if (measuredValue <= 0) {
            return Optional.empty();
        }

        return GrowthStandardTable.lookup(metric, sex, ageMonths).map(standard -> {
            double zScore = toZScore(measuredValue, standard);
            double percentile = normalCdf(zScore) * 100.0;
            return new GrowthPercentileResult(
                    metric,
                    ageMonths,
                    measuredValue,
                    standard.m(),
                    round(zScore, 2),
                    round(percentile, 1));
        });
    }

    private static double toZScore(double value, GrowthStandard standard) {
        double l = standard.l();
        double m = standard.m();
        double s = standard.s();

        if (Math.abs(l) < 1e-9) {
            return Math.log(value / m) / s;
        }
        return (Math.pow(value / m, l) - 1) / (l * s);
    }

    /** 표준정규 누적분포함수. Abramowitz &amp; Stegun 7.1.26 근사식을 사용한다 (오차 &lt; 1.5e-7). */
    static double normalCdf(double z) {
        return 0.5 * (1.0 + erf(z / Math.sqrt(2.0)));
    }

    private static double erf(double x) {
        double sign = Math.signum(x);
        double absX = Math.abs(x);

        double a1 = 0.254829592, a2 = -0.284496736, a3 = 1.421413741;
        double a4 = -1.453152027, a5 = 1.061405429, p = 0.3275911;

        double t = 1.0 / (1.0 + p * absX);
        double y = 1.0 - (((((a5 * t + a4) * t) + a3) * t + a2) * t + a1) * t * Math.exp(-absX * absX);

        return sign * y;
    }

    private static double round(double value, int decimals) {
        double factor = Math.pow(10, decimals);
        return Math.round(value * factor) / factor;
    }
}
