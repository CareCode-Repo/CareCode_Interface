package com.carecode.core.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;

/**
 * API 버전 협상. 버전은 경로가 아니라 {@code X-API-Version} 헤더로 주고받는다.
 *
 * <p>경로 버전({@code /api/v1/...})을 쓰지 않는 이유:
 * <ul>
 *   <li>프런트가 부르는 모든 경로와 SecurityConfig 의 인가 규칙이 버전 없는 경로로 짜여 있다.
 *       경로를 바꾸면 인가 규칙을 전부 두 벌로 유지해야 하고, 한쪽만 고치면 그대로 구멍이 된다.</li>
 *   <li>버전이 갈리는 건 일부 API 뿐이다. 헤더 방식은 바뀐 API 만 새 버전 처리를 두면 된다.</li>
 * </ul>
 *
 * <p>규칙 ({@code docs/reference/api-versioning.md}):
 * <ul>
 *   <li>헤더가 없으면 현재 버전({@value #CURRENT})으로 처리한다. 기존 클라이언트는 아무것도 바꾸지 않아도 된다.</li>
 *   <li>{@code 1}, {@code v1} 모두 받는다. 지원하지 않는 버전은 400 으로 거절한다 — 다른 버전을 기대한
 *       클라이언트에게 조용히 현재 버전 응답을 주면 필드가 어긋나도 알아채지 못한다.</li>
 *   <li>응답에는 항상 실제로 처리한 버전을 {@code X-API-Version} 으로 돌려준다.</li>
 * </ul>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class ApiVersionFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-API-Version";
    public static final int CURRENT = 1;
    public static final Set<Integer> SUPPORTED = Set.of(1);

    /** 컨트롤러에서 처리 중인 버전을 꺼낼 때 쓴다. 새 버전이 생겨 응답이 갈릴 때 필요하다. */
    public static final String REQUEST_ATTRIBUTE = ApiVersionFilter.class.getName() + ".version";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String raw = request.getHeader(HEADER);
        Integer version = parse(raw);

        if (version == null || !SUPPORTED.contains(version)) {
            reject(response);
            return;
        }

        request.setAttribute(REQUEST_ATTRIBUTE, version);
        response.setHeader(HEADER, String.valueOf(version));
        filterChain.doFilter(request, response);
    }

    /** 헤더가 없으면 현재 버전. 형식이 틀리면 null. */
    static Integer parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return CURRENT;
        }
        String value = raw.trim().toLowerCase(Locale.ROOT);
        if (value.startsWith("v")) {
            value = value.substring(1);
        }
        if (!value.matches("[0-9]{1,3}")) {
            return null;
        }
        return Integer.parseInt(value);
    }

    private void reject(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setHeader(HEADER, String.valueOf(CURRENT));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        // 헤더 값은 응답에 되돌려 쓰지 않는다. 그대로 넣으면 JSON 을 깨뜨리거나 주입에 쓰일 수 있다.
        response.getWriter().write("{\"code\":\"API_VERSION_UNSUPPORTED\","
                + "\"message\":\"지원하지 않는 API 버전입니다. X-API-Version 헤더를 빼거나 지원 버전을 지정하세요.\","
                + "\"supportedVersions\":" + SUPPORTED.stream().sorted().toList() + "}");
    }
}
