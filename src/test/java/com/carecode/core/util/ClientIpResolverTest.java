package com.carecode.core.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

/** X-Forwarded-For 신뢰 정책에 대한 회귀 테스트. 과거에는 이 헤더를 무조건 신뢰해서. */
@DisplayName("ClientIpResolver")
class ClientIpResolverTest {

    @Test
    @DisplayName("프록시를 신뢰하지 않으면 X-Forwarded-For 를 무시한다")
    void ignoresForwardedHeaderWhenNotTrusted() {
        ClientIpResolver resolver = new ClientIpResolver(false);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("X-Forwarded-For", "1.2.3.4");

        assertThat(resolver.resolve(request)).isEqualTo("10.0.0.1");
    }

    @Test
    @DisplayName("헤더를 위조해도 신뢰하지 않는 설정에서는 키가 달라지지 않는다")
    void spoofedHeaderCannotChangeIdentity() {
        ClientIpResolver resolver = new ClientIpResolver(false);

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
        ClientIpResolver resolver = new ClientIpResolver(true);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("X-Forwarded-For", "1.2.3.4, 10.0.0.2, 10.0.0.3");

        assertThat(resolver.resolve(request)).isEqualTo("1.2.3.4");
    }

    @Test
    @DisplayName("신뢰 설정이어도 헤더가 없으면 원격 주소를 사용한다")
    void fallsBackToRemoteAddr() {
        ClientIpResolver resolver = new ClientIpResolver(true);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");

        assertThat(resolver.resolve(request)).isEqualTo("10.0.0.1");
    }
}
