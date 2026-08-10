package com.carecode.core.monitoring;

import com.carecode.core.util.LoggingUtil;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@DisplayName("요청 추적 ID")
class TraceIdFilterTest {

    private final TraceIdFilter filter = new TraceIdFilter();

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    @DisplayName("헤더가 없으면 새로 만들어 응답으로 돌려준다")
    void generatesWhenAbsent() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(new MockHttpServletRequest(), response, mock(FilterChain.class));

        assertThat(response.getHeader(TraceIdFilter.TRACE_ID_HEADER)).isNotBlank();
    }

    @Test
    @DisplayName("들어온 추적 ID 를 이어받는다")
    void reusesInboundId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(TraceIdFilter.TRACE_ID_HEADER, "gateway-abc123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, mock(FilterChain.class));

        // 게이트웨이가 붙인 값을 그대로 써야 같은 요청으로 묶인다.
        assertThat(response.getHeader(TraceIdFilter.TRACE_ID_HEADER)).isEqualTo("gateway-abc123");
    }

    @Test
    @DisplayName("체인이 도는 동안 MDC 에서 추적 ID 를 읽을 수 있다")
    void exposesIdDuringChain() throws Exception {
        AtomicReference<String> seen = new AtomicReference<>();
        FilterChain chain = (req, res) -> seen.set(LoggingUtil.getTraceId());
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(new MockHttpServletRequest(), response, chain);

        assertThat(seen.get()).isEqualTo(response.getHeader(TraceIdFilter.TRACE_ID_HEADER));
    }

    @Test
    @DisplayName("요청이 끝나면 MDC 를 비운다")
    void clearsAfterRequest() throws Exception {
        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), mock(FilterChain.class));

        // 톰캣은 스레드를 재사용한다. 남겨두면 다음 요청 로그에 남의 추적 ID 가 붙는다.
        assertThat(LoggingUtil.getTraceId()).isNull();
    }

    @Test
    @DisplayName("체인에서 예외가 나도 MDC 를 비운다")
    void clearsOnException() {
        FilterChain exploding = (req, res) -> {
            throw new IllegalStateException("boom");
        };

        try {
            filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), exploding);
        } catch (Exception ignored) {
            // 예외 자체는 이 테스트의 관심사가 아니다.
        }

        assertThat(LoggingUtil.getTraceId()).isNull();
    }

    @Test
    @DisplayName("로그를 위조할 수 있는 문자는 걸러낸다")
    void sanitizesInboundId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        // 개행이 그대로 들어가면 로그 한 줄을 통째로 지어낼 수 있다.
        request.addHeader(TraceIdFilter.TRACE_ID_HEADER, "abc\n{\"level\":\"ERROR\"}");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, mock(FilterChain.class));

        String traceId = response.getHeader(TraceIdFilter.TRACE_ID_HEADER);
        assertThat(traceId).doesNotContain("\n").doesNotContain("{").doesNotContain("\"");
    }

    @Test
    @DisplayName("지나치게 긴 값은 잘라낸다")
    void truncatesLongId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(TraceIdFilter.TRACE_ID_HEADER, "x".repeat(500));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, mock(FilterChain.class));

        assertThat(response.getHeader(TraceIdFilter.TRACE_ID_HEADER)).hasSize(64);
    }
}
