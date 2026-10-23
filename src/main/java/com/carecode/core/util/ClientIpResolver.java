package com.carecode.core.util;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

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

    /** 경고를 이 간격에 한 번만 남긴다. 직접 노출이면 모든 요청이 해당되므로 로그가 쓸려 나간다. */
    private static final Duration WARN_INTERVAL = Duration.ofMinutes(10);

    private final boolean trustForwardedHeaders;

    /**
     * 프록시를 거치지 않고 도착한 요청 수.
     *
     * <p>XFF 를 신뢰하도록 설정했다는 것은 "프록시 뒤에 있다" 는 전제다. 그런데 XFF 가 없는
     * 요청이 오면 그 요청은 <b>프록시를 거치지 않았다.</b> 즉 앱 포트가 직접 닿는 상태이고,
     * 그러면 공격자가 XFF 를 직접 붙여 IP 기준 제한(로그인·인증코드 발송)을 우회할 수 있다.
     *
     * <p>보안그룹·방화벽 설정은 애플리케이션이 알 수 없다. 이 값은 그 전제가 깨졌는지를
     * <b>런타임에 알 수 있는 유일한 신호</b>다. 0 이 아니면 8082 가 외부에 열려 있다는 뜻이다.
     * 이슈 #90
     */
    private final Counter unproxiedRequests;

    private final AtomicReference<Instant> lastWarnedAt = new AtomicReference<>(Instant.EPOCH);

    public ClientIpResolver(
            @Value("${app.rate-limit.trust-forwarded-headers:false}") boolean trustForwardedHeaders,
            MeterRegistry meterRegistry) {
        this.trustForwardedHeaders = trustForwardedHeaders;
        this.unproxiedRequests = Counter.builder("carecode.ratelimit.unproxied.requests")
                .description("XFF 를 신뢰하도록 설정했는데 XFF 없이 도착한 요청 수. 0 이 아니면 앱 포트가 직접 노출된 것이다")
                .register(meterRegistry);
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
            reportUnproxied(request);
            return request.getRemoteAddr();
        }

        // "client, proxy1, proxy2" 형식에서 최초 클라이언트 주소를 사용한다.
        String first = forwarded.split(",")[0].trim();
        return first.isEmpty() ? request.getRemoteAddr() : first;
    }

    /**
     * 프록시를 거치지 않은 요청을 센다.
     *
     * <p>루프백은 빼고 센다. 배포 스크립트가 서버 안에서 {@code 127.0.0.1:8082} 로 헬스체크를
     * 48번까지 돌리므로, 포함하면 배포마다 경고가 쏟아져 신호가 묻힌다.
     */
    private void reportUnproxied(HttpServletRequest request) {
        String remote = request.getRemoteAddr();
        if (remote == null || remote.startsWith("127.") || "::1".equals(remote) || "0:0:0:0:0:0:0:1".equals(remote)) {
            return;
        }

        unproxiedRequests.increment();

        Instant now = Instant.now();
        Instant last = lastWarnedAt.get();
        if (Duration.between(last, now).compareTo(WARN_INTERVAL) >= 0
                && lastWarnedAt.compareAndSet(last, now)) {
            log.warn("프록시를 거치지 않은 요청이 도착했습니다 - remoteAddr={}, uri={}."
                            + " XFF 를 신뢰하도록 설정했는데 앱 포트가 직접 닿는 상태라면,"
                            + " 헤더를 위조해 IP 기준 제한을 우회할 수 있습니다. 보안그룹/방화벽을 확인하세요.",
                    remote, request.getRequestURI());
        }
    }
}
