package com.carecode.core.util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 클라이언트 IP 해석기. X-Forwarded-For 는 클라이언트가 임의로 붙일 수 있는 헤더다.
 *
 * <p>주의: 이 클래스의 설정만으로 XFF 신뢰 여부가 결정되지 않는다.
 * {@code server.forward-headers-strategy: framework} 가 켜져 있으면 Spring 의
 * ForwardedHeaderFilter 가 요청을 감싸 {@code getRemoteAddr()} 자체를 헤더의 클라이언트
 * 주소로 바꿔친다. 그래서 {@code trustForwardedHeaders=false} 의 폴백 경로도 이미
 * 헤더에서 온 값을 돌려준다. 실제 동작은 ForwardedClientIpTest 에 고정해 두었다.
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
