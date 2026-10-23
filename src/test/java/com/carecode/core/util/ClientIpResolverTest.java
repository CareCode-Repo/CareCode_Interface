package com.carecode.core.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

/** X-Forwarded-For 신뢰 정책에 대한 회귀 테스트. 과거에는 이 헤더를 무조건 신뢰해서. */
@DisplayName("ClientIpResolver")
class ClientIpResolverTest {

    @Test
    @DisplayName("프록시를 신뢰하지 않으면 X-Forwarded-For 를 무시한다")
    void ignoresForwardedHeaderWhenNotTrusted() {
        ClientIpResolver resolver = new ClientIpResolver(false, new SimpleMeterRegistry());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("X-Forwarded-For", "1.2.3.4");

        assertThat(resolver.resolve(request)).isEqualTo("10.0.0.1");
    }

    @Test
    @DisplayName("헤더를 위조해도 신뢰하지 않는 설정에서는 키가 달라지지 않는다")
    void spoofedHeaderCannotChangeIdentity() {
        ClientIpResolver resolver = new ClientIpResolver(false, new SimpleMeterRegistry());

        MockHttpServletRequest first = new MockHttpServletRequest();
        first.setRemoteAddr("10.0.0.1");
        first.addHeader("X-Forwarded-For", "1.1.1.1");

        MockHttpServletRequest second = new MockHttpServletRequest();
        second.setRemoteAddr("10.0.0.1");
        second.addHeader("X-Forwarded-For", "2.2.2.2");

        assertThat(resolver.resolve(first)).isEqualTo(resolver.resolve(second));
    }

    @Test
    @DisplayName("프록시를 신뢰하면 X-Forwarded-For 의 첫 주소를 사용한다")
    void usesFirstForwardedAddressWhenTrusted() {
        ClientIpResolver resolver = new ClientIpResolver(true, new SimpleMeterRegistry());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("X-Forwarded-For", "1.2.3.4, 10.0.0.2, 10.0.0.3");

        assertThat(resolver.resolve(request)).isEqualTo("1.2.3.4");
    }

    @Test
    @DisplayName("신뢰 설정이어도 헤더가 없으면 원격 주소를 사용한다")
    void fallsBackToRemoteAddr() {
        ClientIpResolver resolver = new ClientIpResolver(true, new SimpleMeterRegistry());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");

        assertThat(resolver.resolve(request)).isEqualTo("10.0.0.1");
    }

    /**
     * 앱 포트가 직접 노출됐는지는 애플리케이션이 알 수 없다. 보안그룹·방화벽은 코드 밖이다.
     *
     * <p>알 수 있는 신호가 하나 있다 — XFF 를 신뢰하도록 설정했다는 것은 "프록시 뒤에 있다" 는
     * 전제인데, XFF 가 없는 요청이 오면 그 요청은 프록시를 거치지 않았다. 그러면 공격자가 XFF 를
     * 직접 붙여 IP 기준 제한을 우회할 수 있다. 그래서 그 수를 지표로 센다. 이슈 #90
     */
    @Test
    @DisplayName("프록시를 거치지 않은 요청을 지표로 센다")
    void countsUnproxiedRequests() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        ClientIpResolver resolver = new ClientIpResolver(true, registry);

        MockHttpServletRequest direct = new MockHttpServletRequest();
        direct.setRemoteAddr("203.0.113.9");   // 외부에서 바로 들어온 요청 (XFF 없음)

        assertThat(resolver.resolve(direct)).isEqualTo("203.0.113.9");
        assertThat(registry.get("carecode.ratelimit.unproxied.requests").counter().count())
                .as("0 이 아니면 앱 포트가 직접 열려 있다는 뜻이다")
                .isEqualTo(1.0);
    }

    /**
     * 배포 스크립트가 서버 안에서 127.0.0.1:8082 로 헬스체크를 48번까지 돌린다. 그것까지 세면
     * 배포마다 지표가 튀어 신호가 묻힌다.
     */
    @Test
    @DisplayName("루프백 요청은 세지 않는다 - 배포 헬스체크가 그 경로다")
    void doesNotCountLoopback() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        ClientIpResolver resolver = new ClientIpResolver(true, registry);

        for (String loopback : new String[] {"127.0.0.1", "::1"}) {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRemoteAddr(loopback);
            resolver.resolve(request);
        }

        assertThat(registry.get("carecode.ratelimit.unproxied.requests").counter().count())
                .isEqualTo(0.0);
    }

    @Test
    @DisplayName("프록시를 거친 요청은 세지 않는다")
    void doesNotCountProxiedRequests() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        ClientIpResolver resolver = new ClientIpResolver(true, registry);

        MockHttpServletRequest proxied = new MockHttpServletRequest();
        proxied.setRemoteAddr("10.0.0.1");
        proxied.addHeader("X-Forwarded-For", "203.0.113.9");

        assertThat(resolver.resolve(proxied)).isEqualTo("203.0.113.9");
        assertThat(registry.get("carecode.ratelimit.unproxied.requests").counter().count())
                .isEqualTo(0.0);
    }
}
