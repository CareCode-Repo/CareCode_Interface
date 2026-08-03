package com.carecode.core.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 목록 API 가 무제한 조회로 되돌아가지 않도록 보장하는 테스트.
 */
@DisplayName("PageRequestUtil")
class PageRequestUtilTest {

    @Test
    @DisplayName("size 가 없으면 기본값을 쓴다")
    void appliesDefaultSize() {
        assertThat(PageRequestUtil.normalizeSize(null)).isEqualTo(PageRequestUtil.DEFAULT_PAGE_SIZE);
    }

    @Test
    @DisplayName("과도한 size 는 상한으로 잘린다")
    void capsExcessiveSize() {
        assertThat(PageRequestUtil.normalizeSize(1_000_000)).isEqualTo(PageRequestUtil.MAX_PAGE_SIZE);
    }

    @Test
    @DisplayName("0 이하 size 는 기본값으로 보정된다")
    void normalizesNonPositiveSize() {
        assertThat(PageRequestUtil.normalizeSize(0)).isEqualTo(PageRequestUtil.DEFAULT_PAGE_SIZE);
        assertThat(PageRequestUtil.normalizeSize(-5)).isEqualTo(PageRequestUtil.DEFAULT_PAGE_SIZE);
    }

    @Test
    @DisplayName("음수 page 는 0 으로 보정된다")
    void normalizesNegativePage() {
        assertThat(PageRequestUtil.normalizePage(-1)).isZero();
        assertThat(PageRequestUtil.normalizePage(null)).isZero();
        assertThat(PageRequestUtil.normalizePage(3)).isEqualTo(3);
    }
}
