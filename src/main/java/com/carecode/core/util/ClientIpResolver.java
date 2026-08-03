package com.carecode.core.util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 클라이언트 IP 해석기.
 *
 * <p>{@code X-Forwarded-For} 는 클라이언트가 임의로 붙일 수 있는 헤더다.
 * 신뢰할 수 있는 리버스 프록시 뒤에 있지 않은데 이 헤더를 그대로 쓰면
 * 헤더 값만 바꿔가며 IP 기반 rate limit 을 무한히 우회할 수 있다.
 *
 * <p>그래서 {@code app.rate-limit.trust-forwarded-headers=true} 인 경우에만
 * 프록시 헤더를 사용하고, 기본값은 TCP 연결의 실제 원격 주소를 쓴다.
 */
@Slf4j
@Component
public class ClientIpResolver {

    private final boolean trustForwardedHeaders;

    public ClientIpResolver(
            @Value("${app.rate-limit.trust-forwarded-headers:false}") boolean trustForwardedHeaders) {
        this.trustForwardedHeaders = trustForwardedHeaders;
        if (trustForwardedHeaders) {
            log.info("X-Forwarded-For 헤더를 신뢰하도록 설정되었습니다. 반드시 신뢰 가능한 프록시 뒤에서만 사용하세요.");
        }
    }

    public String resolve(HttpServletRequest request) {
        if (!trustForwardedHeaders) {
            return request.getRemoteAddr();
        }

        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded == null || forwarded.isBlank() || "unknown".equalsIgnoreCase(forwarded)) {
            return request.getRemoteAddr();
        }

        // "client, proxy1, proxy2" 형식에서 최초 클라이언트 주소를 사용한다.
        String first = forwarded.split(",")[0].trim();
        return first.isEmpty() ? request.getRemoteAddr() : first;
    }
}
