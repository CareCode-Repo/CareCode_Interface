package com.carecode.core.monitoring;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/** 요청별 쿼리 수를 집계해 임계치를 넘으면 경고한다. 어느 엔드포인트가 N+1 인지 로그로 드러난다. */
@Slf4j
@RequiredArgsConstructor
public class QueryCountFilter extends OncePerRequestFilter {

    private final int threshold;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        QueryCountHolder.start();
        long startedAt = System.nanoTime();
        try {
            chain.doFilter(request, response);
        } finally {
            int queryCount = QueryCountHolder.get();
            QueryCountHolder.clear();

            long elapsedMs = (System.nanoTime() - startedAt) / 1_000_000;
            String endpoint = request.getMethod() + " " + request.getRequestURI();
            if (queryCount >= threshold) {
                log.warn("N+1 의심 - {} 쿼리 {}건, {}ms (임계치 {}건)", endpoint, queryCount, elapsedMs, threshold);
            } else if (queryCount > 0) {
                log.debug("쿼리 측정 - {} 쿼리 {}건, {}ms", endpoint, queryCount, elapsedMs);
            }
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator") || path.startsWith("/swagger-ui") || path.startsWith("/v3/api-docs");
    }
}
