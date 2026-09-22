package com.carecode.core.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("API 버전 헤더")
class ApiVersionFilterTest {

    private final ApiVersionFilter filter = new ApiVersionFilter();

    @Test
    @DisplayName("헤더가 없으면 현재 버전으로 처리하고 응답에 버전을 알린다 — 기존 클라이언트는 그대로 동작")
    void missingHeaderMeansCurrent() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/facilities");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).as("다음 필터로 넘어간다").isNotNull();
        assertThat(response.getHeader(ApiVersionFilter.HEADER)).isEqualTo("1");
        assertThat(request.getAttribute(ApiVersionFilter.REQUEST_ATTRIBUTE)).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"1", "v1", "V1", " 1 "})
    void acceptsSupportedForms(String header) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/facilities");
        request.addHeader(ApiVersionFilter.HEADER, header);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @ParameterizedTest
    @ValueSource(strings = {"2", "v9", "abc", "1.0", "<script>"})
    @DisplayName("지원하지 않거나 형식이 틀린 버전은 400 이고 컨트롤러까지 가지 않는다")
    void rejectsUnsupported(String header) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/facilities");
        request.addHeader(ApiVersionFilter.HEADER, header);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).as("다음 필터로 넘어가지 않는다").isNull();
        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getContentAsString()).contains("API_VERSION_UNSUPPORTED").doesNotContain(header.trim());
    }
}
