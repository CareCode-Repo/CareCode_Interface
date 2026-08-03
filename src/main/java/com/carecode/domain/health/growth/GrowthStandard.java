package com.carecode.domain.health.growth;

/**
 * WHO 아동 성장 표준의 LMS 파라미터.
 *
 * <p>WHO 는 연령·성별별로 L(왜도), M(중앙값), S(변동계수) 세 값을 제공하며,
 * 이 값으로 개별 측정치의 Z-score 와 백분위를 계산한다.
 *
 * @param ageMonths 생후 개월 수
 * @param l         Box-Cox 변환 지수
 * @param m         중앙값
 * @param s         변동계수
 */
public record GrowthStandard(int ageMonths, double l, double m, double s) {
}
