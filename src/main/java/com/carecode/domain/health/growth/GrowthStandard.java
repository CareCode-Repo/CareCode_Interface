package com.carecode.domain.health.growth;

/** WHO 아동 성장 표준의 LMS 파라미터. WHO 는 연령·성별별로 L(왜도), M(중앙값), S(변동계수) 세 값을 제공하며 */
public record GrowthStandard(int ageMonths, double l, double m, double s) {
}
