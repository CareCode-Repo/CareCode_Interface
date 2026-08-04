package com.carecode.core.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("지원대상 연령 파싱")
class AgeRangeParserTest {

    @Test
    @DisplayName("개월 범위를 그대로 읽는다")
    void parsesMonthRange() {
        AgeRangeParser.AgeRange range = AgeRangeParser.parse("생후 12~23개월 아동");

        assertThat(range.minMonths()).isEqualTo(12);
        assertThat(range.maxMonths()).isEqualTo(23);
    }

    @Test
    @DisplayName("세 범위는 개월로 환산하고 상한은 해당 연도의 마지막 달로 잡는다")
    void convertsYearRangeToMonths() {
        AgeRangeParser.AgeRange range = AgeRangeParser.parse("만 3~5세 유아");

        assertThat(range.minMonths()).isEqualTo(36);
        assertThat(range.maxMonths()).isEqualTo(71);
    }

    @Test
    @DisplayName("만 N세 사이에 만이 반복돼도 인식한다")
    void parsesRepeatedManPrefix() {
        AgeRangeParser.AgeRange range = AgeRangeParser.parse("만 0세 ~ 만 2세");

        assertThat(range.minMonths()).isZero();
        assertThat(range.maxMonths()).isEqualTo(35);
    }

    @Test
    @DisplayName("미만은 상한을 한 단위 낮춘다")
    void parsesExclusiveUpperBound() {
        AgeRangeParser.AgeRange range = AgeRangeParser.parse("만 7세 미만 아동");

        assertThat(range.minMonths()).isZero();
        assertThat(range.maxMonths()).isEqualTo(83);
    }

    @Test
    @DisplayName("이하는 해당 연도 끝까지 포함한다")
    void parsesInclusiveUpperBound() {
        AgeRangeParser.AgeRange range = AgeRangeParser.parse("만 8세 이하");

        assertThat(range.maxMonths()).isEqualTo(107);
    }

    @Test
    @DisplayName("이상은 상한을 두지 않는다")
    void parsesLowerBoundOnly() {
        AgeRangeParser.AgeRange range = AgeRangeParser.parse("만 6세 이상 아동");

        assertThat(range.minMonths()).isEqualTo(72);
        assertThat(range.maxMonths()).isNull();
    }

    @Test
    @DisplayName("개월 미만 표기도 처리한다")
    void parsesMonthBound() {
        AgeRangeParser.AgeRange range = AgeRangeParser.parse("24개월 미만 영아");

        assertThat(range.maxMonths()).isEqualTo(23);
    }

    @Test
    @DisplayName("연령 조건이 없으면 추정하지 않는다")
    void returnsNullWhenNoAgeCondition() {
        assertThat(AgeRangeParser.parse("임신부 및 산모")).isNull();
        assertThat(AgeRangeParser.parse("")).isNull();
        assertThat(AgeRangeParser.parse(null)).isNull();
    }

    @Test
    @DisplayName("연령으로 보기 어려운 큰 숫자는 무시한다")
    void ignoresImplausibleNumbers() {
        assertThat(AgeRangeParser.parse("2024~2025세")).isNull();
        assertThat(AgeRangeParser.parse("500개월 미만")).isNull();
    }

    @Test
    @DisplayName("역순 범위는 잘못된 표기로 보고 버린다")
    void rejectsInvertedRange() {
        assertThat(AgeRangeParser.parse("5~3세")).isNull();
    }
}
