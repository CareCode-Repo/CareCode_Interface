package com.carecode.core.monitoring;

import com.carecode.core.util.LoggingUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 요청 하나에 추적 ID 하나를 붙인다.
 *
 * <p>MDC 는 예전부터 로그로 나가고 있었지만 traceId 를 넣는 곳이 {@code @LogExecutionTime} 안뿐이라,
 * 인증 실패나 없는 경로처럼 컨트롤러에 닿기 전에 끝나는 요청에는 아무 값도 없었다. 실제로 500 원인을
 * 찾을 때 타임스탬프로 로그를 뒤져야 했다.
 *
 * <p>보안 필터보다 먼저 돌려 401 로그에도 ID 가 남게 하고, 응답 헤더로 돌려줘서 사용자가 화면에
 * 뜬 값을 그대로 알려주면 바로 찾을 수 있게 한다.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    public static final String TRACE_ID_HEADER = "X-Request-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 로드밸런서나 게이트웨이가 이미 붙였다면 그대로 이어받아야 같은 요청으로 묶인다.
        String inbound = request.getHeader(TRACE_ID_HEADER);
        String traceId = StringUtils.hasText(inbound)
                ? sanitize(inbound)
                : LoggingUtil.generateTraceId();

        LoggingUtil.setTraceId(traceId);
        // 오류 화면에 띄운 값을 사용자가 그대로 불러 주면 로그에서 바로 찾을 수 있다.
        response.setHeader(TRACE_ID_HEADER, traceId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            // 톰캣은 스레드를 재사용한다. 비우지 않으면 다음 요청 로그에 남의 추적 ID 가 붙는다.
            LoggingUtil.clear();
        }
    }

    /**
     * 외부에서 온 값은 그대로 믿지 않는다.
     *
     * <p>로그에 그대로 들어가므로 개행이 섞이면 한 줄을 위조해 다른 요청인 것처럼 꾸밀 수 있다.
     * 길이도 제한해 로그가 헤더로 부풀지 않게 한다.
     */
    private String sanitize(String value) {
        String cleaned = value.replaceAll("[^A-Za-z0-9._-]", "");
        if (cleaned.isEmpty()) {
            return LoggingUtil.generateTraceId();
        }
        return cleaned.length() > 64 ? cleaned.substring(0, 64) : cleaned;
    }
}
