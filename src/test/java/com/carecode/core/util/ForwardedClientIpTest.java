package com.carecode.core.util;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.filter.ForwardedHeaderFilter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 프록시 뒤에서 클라이언트 IP 가 실제로 어떻게 결정되는지 고정한다.
 *
 * <p>이 프로젝트에는 X-Forwarded-For 를 다루는 지점이 두 곳 있고, 둘의 관계가
 * 설정만 봐서는 드러나지 않는다.
 *
 * <ol>
 *   <li>{@code server.forward-headers-strategy: framework} — Spring 이
 *       {@link ForwardedHeaderFilter} 를 등록한다. <b>이 필터가 요청을 감싸서
 *       {@code getRemoteAddr()} 자체를 XFF 의 클라이언트 주소로 바꿔친다.</b></li>
 *   <li>{@link ClientIpResolver} 의 {@code app.rate-limit.trust-forwarded-headers} —
 *       XFF 를 직접 읽을지 말지를 정한다.</li>
 * </ol>
 *
 * <p>중요한 것은 (1)이 이미 켜져 있으면 (2)를 꺼도 XFF 를 신뢰하는 결과가 된다는 점이다.
 * {@code trust=false} 의 폴백인 {@code getRemoteAddr()} 이 이미 필터가 바꿔친 값이기 때문이다.
 * 즉 <b>실질적인 XFF 신뢰 스위치는 {@code server.forward-headers-strategy} 쪽</b>이고,
 * {@code trust-forwarded-headers} 는 이름이 시사하는 만큼의 통제력을 갖고 있지 않다.
 *
 * <p>이 사실을 모르면 "trust=false 니까 헤더 위조에 안전하다" 고 잘못 판단하게 된다.
 * 그래서 추측 대신 여기에 실제 동작을 박아둔다. 관련 정리는 이슈 #90.
 */
@DisplayName("프록시 뒤 클라이언트 IP 해석")
class ForwardedClientIpTest {

    private static final String PROXY_IP = "10.0.0.5";
    private static final String CLIENT_IP = "203.0.113.77";

    private MockHttpServletRequest requestBehindProxy() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/auth/login");
        request.setRemoteAddr(PROXY_IP);
        request.addHeader("X-Forwarded-For", CLIENT_IP);
        request.addHeader("X-Forwarded-Proto", "https");
        request.addHeader("Host", "api.example.com");
        return request;
    }

    /** ForwardedHeaderFilter 통과 후의 요청을 돌려준다. 운영에서 컨트롤러가 보는 요청이다. */
    private HttpServletRequest afterForwardedFilter(MockHttpServletRequest request) throws Exception {
        ForwardedHeaderFilter filter = new ForwardedHeaderFilter();
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, new MockHttpServletResponse(), chain);
        return (HttpServletRequest) chain.getRequest();
    }

    @Test
    @DisplayName("ForwardedHeaderFilter 가 remote addr 을 XFF 의 클라이언트 주소로 바꿔친다")
    void forwardedFilterRewritesRemoteAddr() throws Exception {
        HttpServletRequest wrapped = afterForwardedFilter(requestBehindProxy());

        assertThat(wrapped.getScheme()).isEqualTo("https");
        assertThat(wrapped.getRemoteAddr())
                .as("필터가 이미 클라이언트 주소를 복원하므로, 이후 단계는 프록시 IP 를 보지 않는다")
                .isEqualTo(CLIENT_IP);
    }

    @Test
    @DisplayName("필터가 켜져 있으면 trust=false 여도 클라이언트별로 구분된다")
    void trustFlagIsRedundantWhenForwardedFilterIsEnabled() throws Exception {
        HttpServletRequest wrapped = afterForwardedFilter(requestBehindProxy());

        // 두 설정이 같은 결과를 낸다. rate limit 이 "프록시 IP 하나로 집계되는" 상황은
        // forward-headers-strategy 가 꺼져 있을 때만 발생한다.
        assertThat(new ClientIpResolver(false).resolve(wrapped)).isEqualTo(CLIENT_IP);
        assertThat(new ClientIpResolver(true).resolve(wrapped)).isEqualTo(CLIENT_IP);
    }

    @Test
    @DisplayName("필터가 없으면 trust 설정이 실제로 갈린다")
    void withoutForwardedFilterTheFlagMatters() {
        MockHttpServletRequest raw = requestBehindProxy();

        assertThat(new ClientIpResolver(false).resolve(raw)).isEqualTo(PROXY_IP);
        assertThat(new ClientIpResolver(true).resolve(raw)).isEqualTo(CLIENT_IP);
    }

    @Test
    @DisplayName("헤더는 위조할 수 있다 — 앱 포트가 프록시 뒤에 있어야만 의미가 있다")
    void forwardedHeaderIsAttackerControlled() throws Exception {
        MockHttpServletRequest direct = new MockHttpServletRequest("POST", "/auth/login");
        direct.setRemoteAddr("198.51.100.9");            // 공격자의 실제 주소
        direct.addHeader("X-Forwarded-For", "1.2.3.4");  // 공격자가 직접 붙인 값

        // 프록시를 거치지 않은 요청에도 필터는 헤더를 그대로 믿는다.
        // 요청마다 값을 바꾸면 IP 기준 제한(로그인 시도, 인증코드 발송)이 무력화된다.
        // 따라서 이 구성은 "앱 포트에 프록시만 접근 가능" 이라는 전제와 한 쌍이다.
        assertThat(afterForwardedFilter(direct).getRemoteAddr()).isEqualTo("1.2.3.4");
    }
}
