package com.carecode.core.client.constants;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("공공데이터 시도 코드")
class PublicDataApiConstantsTest {

    @Test
    @DisplayName("정식 명칭으로 조회한다")
    void findsByFullName() {
        assertThat(PublicDataApiConstants.findSidoCode("서울특별시")).isEqualTo("11");
        assertThat(PublicDataApiConstants.findSidoCode("경기도")).isEqualTo("31");
    }

    @Test
    @DisplayName("축약 표기도 매칭한다")
    void findsByShortName() {
        assertThat(PublicDataApiConstants.findSidoCode("서울")).isEqualTo("11");
        assertThat(PublicDataApiConstants.findSidoCode("제주")).isEqualTo("39");
    }

    @Test
    @DisplayName("알 수 없는 지역은 null을 반환한다")
    void returnsNullForUnknown() {
        assertThat(PublicDataApiConstants.findSidoCode("존재하지않는시")).isNull();
        assertThat(PublicDataApiConstants.findSidoCode("")).isNull();
        assertThat(PublicDataApiConstants.findSidoCode(null)).isNull();
    }

    @Test
    @DisplayName("전국 17개 시도가 정의돼 있다")
    void coversAllProvinces() {
        assertThat(PublicDataApiConstants.SIDO_CODES).hasSize(17);
    }
}
