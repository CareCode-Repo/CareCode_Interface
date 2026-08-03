package com.carecode.domain.health.growth;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * WHO 아동 성장 표준(0~60개월) LMS 표.
 *
 * <p>출처: WHO Child Growth Standards (weight-for-age, length/height-for-age).
 * 표는 6개월 간격 발췌본이며, 사이 값은 선형 보간한다.
 * 정밀한 임상 판단이 필요하면 WHO 전체 표를 적재해 대체할 수 있다.
 *
 * <p>주의: 백분위는 참고 지표다. 진단은 의료진의 판단을 따른다.
 */
public final class GrowthStandardTable {

    /** 남아 체중(kg) for age. */
    private static final List<GrowthStandard> BOY_WEIGHT = List.of(
            new GrowthStandard(0, 0.3487, 3.3464, 0.14602),
            new GrowthStandard(6, 0.1738, 7.9340, 0.12385),
            new GrowthStandard(12, 0.0402, 9.6479, 0.12106),
            new GrowthStandard(18, -0.0756, 10.9385, 0.12237),
            new GrowthStandard(24, -0.1733, 12.1515, 0.12456),
            new GrowthStandard(30, -0.2565, 13.3000, 0.12718),
            new GrowthStandard(36, -0.3277, 14.3429, 0.12988),
            new GrowthStandard(42, -0.3891, 15.3160, 0.13257),
            new GrowthStandard(48, -0.4425, 16.3497, 0.13519),
            new GrowthStandard(54, -0.4894, 17.3140, 0.13774),
            new GrowthStandard(60, -0.5308, 18.3074, 0.14021));

    /** 여아 체중(kg) for age. */
    private static final List<GrowthStandard> GIRL_WEIGHT = List.of(
            new GrowthStandard(0, 0.3809, 3.2322, 0.14171),
            new GrowthStandard(6, 0.1002, 7.2970, 0.12619),
            new GrowthStandard(12, -0.0756, 8.9481, 0.12839),
            new GrowthStandard(18, -0.1972, 10.2315, 0.13089),
            new GrowthStandard(24, -0.2890, 11.4775, 0.13341),
            new GrowthStandard(30, -0.3624, 12.6489, 0.13587),
            new GrowthStandard(36, -0.4232, 13.7626, 0.13827),
            new GrowthStandard(42, -0.4750, 14.8442, 0.14061),
            new GrowthStandard(48, -0.5199, 15.9036, 0.14290),
            new GrowthStandard(54, -0.5594, 16.9481, 0.14515),
            new GrowthStandard(60, -0.5946, 17.9873, 0.14738));

    /** 남아 신장(cm) for age. */
    private static final List<GrowthStandard> BOY_HEIGHT = List.of(
            new GrowthStandard(0, 1.0, 49.8842, 0.03795),
            new GrowthStandard(6, 1.0, 67.6236, 0.03165),
            new GrowthStandard(12, 1.0, 75.7488, 0.03317),
            new GrowthStandard(18, 1.0, 82.2587, 0.03468),
            new GrowthStandard(24, 1.0, 87.8161, 0.03610),
            new GrowthStandard(30, 1.0, 92.1131, 0.03765),
            new GrowthStandard(36, 1.0, 96.0835, 0.03902),
            new GrowthStandard(42, 1.0, 99.8003, 0.04026),
            new GrowthStandard(48, 1.0, 103.3273, 0.04141),
            new GrowthStandard(54, 1.0, 106.7050, 0.04250),
            new GrowthStandard(60, 1.0, 110.0000, 0.04352));

    /** 여아 신장(cm) for age. */
    private static final List<GrowthStandard> GIRL_HEIGHT = List.of(
            new GrowthStandard(0, 1.0, 49.1477, 0.03790),
            new GrowthStandard(6, 1.0, 65.7311, 0.03395),
            new GrowthStandard(12, 1.0, 74.0150, 0.03568),
            new GrowthStandard(18, 1.0, 80.7079, 0.03737),
            new GrowthStandard(24, 1.0, 86.4153, 0.03894),
            new GrowthStandard(30, 1.0, 90.9915, 0.04039),
            new GrowthStandard(36, 1.0, 95.0515, 0.04171),
            new GrowthStandard(42, 1.0, 98.7680, 0.04291),
            new GrowthStandard(48, 1.0, 102.2665, 0.04401),
            new GrowthStandard(54, 1.0, 105.6003, 0.04503),
            new GrowthStandard(60, 1.0, 108.7900, 0.04598));

    private static final Map<Key, List<GrowthStandard>> TABLES = Map.of(
            new Key(GrowthMetric.WEIGHT, Sex.MALE), BOY_WEIGHT,
            new Key(GrowthMetric.WEIGHT, Sex.FEMALE), GIRL_WEIGHT,
            new Key(GrowthMetric.HEIGHT, Sex.MALE), BOY_HEIGHT,
            new Key(GrowthMetric.HEIGHT, Sex.FEMALE), GIRL_HEIGHT);

    private GrowthStandardTable() {
    }

    /**
     * 해당 연령의 LMS 값을 구한다. 표에 없는 개월 수는 인접 구간을 선형 보간한다.
     *
     * @return 표 범위를 벗어나면 {@link Optional#empty()}
     */
    public static Optional<GrowthStandard> lookup(GrowthMetric metric, Sex sex, int ageMonths) {
        List<GrowthStandard> table = TABLES.get(new Key(metric, sex));
        if (table == null || ageMonths < 0) {
            return Optional.empty();
        }

        int maxAge = table.get(table.size() - 1).ageMonths();
        if (ageMonths > maxAge) {
            // 60개월을 넘어가면 WHO 성장 표준의 적용 범위를 벗어난다.
            return Optional.empty();
        }

        for (int i = 0; i < table.size(); i++) {
            GrowthStandard current = table.get(i);
            if (current.ageMonths() == ageMonths) {
                return Optional.of(current);
            }
            if (current.ageMonths() > ageMonths) {
                GrowthStandard previous = table.get(i - 1);
                return Optional.of(interpolate(previous, current, ageMonths));
            }
        }
        return Optional.empty();
    }

    private static GrowthStandard interpolate(GrowthStandard low, GrowthStandard high, int ageMonths) {
        double span = high.ageMonths() - low.ageMonths();
        double ratio = (ageMonths - low.ageMonths()) / span;
        return new GrowthStandard(
                ageMonths,
                low.l() + (high.l() - low.l()) * ratio,
                low.m() + (high.m() - low.m()) * ratio,
                low.s() + (high.s() - low.s()) * ratio);
    }

    private record Key(GrowthMetric metric, Sex sex) {
    }
}
