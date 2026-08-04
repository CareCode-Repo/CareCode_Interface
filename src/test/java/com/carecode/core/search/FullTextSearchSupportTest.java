package com.carecode.core.search;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("전문 검색 키워드 정규화")
class FullTextSearchSupportTest {

    private final FullTextSearchSupport support = new FullTextSearchSupport(true);

    @Test
    @DisplayName("불리언 모드 연산자를 제거한다")
    void stripsBooleanOperators() {
        assertThat(support.normalize("강남 +어린이집 -사립")).isEqualTo("강남 어린이집 사립");
    }

    @Test
    @DisplayName("한 글자 키워드는 전문 검색으로 처리하지 않는다")
    void rejectsTooShortKeyword() {
        assertThat(support.normalize("가")).isNull();
        assertThat(support.canUseFullText("가")).isFalse();
    }

    @Test
    @DisplayName("연산자만 있는 키워드는 무효로 본다")
    void rejectsOperatorOnlyKeyword() {
        assertThat(support.normalize("+++")).isNull();
    }

    @Test
    @DisplayName("비활성화하면 항상 LIKE 로 폴백한다")
    void fallsBackWhenDisabled() {
        FullTextSearchSupport disabled = new FullTextSearchSupport(false);

        assertThat(disabled.canUseFullText("어린이집")).isFalse();
    }
}
