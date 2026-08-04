package com.carecode.domain.health.growth;

/**
 * 성장 백분위 계산 결과.
 *
 * @param metric        지표 종류
 * @param ageMonths     측정 시점의 생후 개월 수
 * @param measuredValue 측정값
 * @param medianValue   같은 연령·성별의 표준 중앙값(50 백분위)
 * @param zScore        표준화 점수
 * @param percentile    백분위 (0~100)
 */
public record GrowthPercentileResult(
        GrowthMetric metric,
        int ageMonths,
        double measuredValue,
        double medianValue,
        double zScore,
        double percentile) {

    /**
     * 임상적 주의가 필요한 범위인지.
     * WHO 는 |Z| &gt; 2 를 주의 구간으로 본다.
     */
    public boolean needsAttention() {
        return Math.abs(zScore) > 2.0;
    }

    public String interpretation() {
        if (zScore < -3) {
            return "표준보다 매우 낮음 (전문의 상담 권장)";
        }
        if (zScore < -2) {
            return "표준보다 낮음 (경과 관찰 필요)";
        }
        if (zScore > 3) {
            return "표준보다 매우 높음 (전문의 상담 권장)";
        }
        if (zScore > 2) {
            return "표준보다 높음 (경과 관찰 필요)";
        }
        return "정상 범위";
    }
}
