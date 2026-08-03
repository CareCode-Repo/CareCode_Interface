package com.carecode.domain.health.growth;

/**
 * 성장 지표 종류.
 */
public enum GrowthMetric {
    WEIGHT("체중", "kg"),
    HEIGHT("신장", "cm");

    private final String displayName;
    private final String unit;

    GrowthMetric(String displayName, String unit) {
        this.displayName = displayName;
        this.unit = unit;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getUnit() {
        return unit;
    }
}
