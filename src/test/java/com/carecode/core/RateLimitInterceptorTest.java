package com.carecode.core;

import com.carecode.core.util.ClientIpResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.when;

/**
 * 전 구간 기본 rate limit 인터셉터 테스트.
 *
 * <p>고정하려는 계약은 두 가지다.
 * <ul>
 *   <li>Redis 가 죽어도 요청은 통과한다(fail-open). 카운터를 못 세는 것 때문에 서비스 전체가
 *       500 이 되면 안 된다. 예전에는 {@code DataAccessException} 만 잡고 TTL 조회는
 *       보호 밖에 있어서, 그 경로의 실패가 그대로 500 이 됐다.</li>
 *   <li>민감 엔드포인트 판정이 실제 경로를 가리킨다. 예전 목록은 이 앱에 존재하지 않는
 *       {@code /api/v1/...} 을 보고 있어서 한 번도 적용된 적이 없었다.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("RateLimitInterceptor - 기본 제한")
class RateLimitInterceptorTest {

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    private RateLimitInterceptor interceptor;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        interceptor = new RateLimitInterceptor(redisTemplate, new ClientIpResolver(false));
        response = new MockHttpServletResponse();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.getExpire(anyString())).thenReturn(60L);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private MockHttpServletRequest request(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
        request.setRemoteAddr("203.0.113.10");
        return request;
    }

    private void login(String email) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        email, null, List.of(new SimpleGrantedAuthority("ROLE_PARENT"))));
    }

    @Test
    @DisplayName("한도 안이면 통과하고 남은 한도를 헤더로 알려준다")
    void allowsWithinLimit() throws Exception {
        when(valueOperations.increment(anyString())).thenReturn(1L);

        assertThat(interceptor.preHandle(request("/community/posts"), response, new Object())).isTrue();
        assertThat(response.getHeader("X-RateLimit-Limit")).isNotNull();
        assertThat(response.getHeader("X-RateLimit-Remaining")).isNotNull();
    }

    @Test
    @DisplayName("한도를 넘으면 429 로 끊는다")
    void blocksOverLimit() throws Exception {
        when(valueOperations.increment(anyString())).thenReturn(9_999L);

        assertThat(interceptor.preHandle(request("/community/posts"), response, new Object())).isFalse();
        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getContentAsString()).contains("RATE_LIMIT_EXCEEDED");
    }

    @Test
    @DisplayName("Redis 연결이 끊겨도 요청은 통과시킨다")
    void failsOpenOnRedisOutage() throws Exception {
        when(valueOperations.increment(anyString()))
                .thenThrow(new RedisConnectionFailureException("connection refused"));

        assertThat(interceptor.preHandle(request("/community/posts"), response, new Object())).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("카운터 자체를 쓸 수 없는 상태(NPE 등)에서도 통과시킨다")
    void failsOpenOnUnexpectedError() throws Exception {
        when(redisTemplate.opsForValue()).thenReturn(null);

        assertThat(interceptor.preHandle(request("/community/posts"), response, new Object())).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("TTL 조회가 실패해도 요청을 막지 않는다")
    void failsOpenOnTtlLookupError() throws Exception {
        when(valueOperations.increment(anyString())).thenReturn(1L);
        when(redisTemplate.getExpire(anyString()))
                .thenThrow(new RedisConnectionFailureException("connection refused"));

        assertThat(interceptor.preHandle(request("/community/posts"), response, new Object())).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("로그인·가입 경로는 IP 기준 낮은 한도를 적용한다")
    void sensitiveEndpointsUseIpKeyAndLowLimit() throws Exception {
        login("member@example.com");
        when(valueOperations.increment(startsWith("ratelimit:ip:"))).thenReturn(1L);

        // 로그인한 상태여도 로그인/가입 경로는 IP 기준으로 센다.
        assertThat(interceptor.preHandle(request("/auth/login"), response, new Object())).isTrue();
        assertThat(response.getHeader("X-RateLimit-Limit")).isEqualTo("30");
    }

    @Test
    @DisplayName("인증 요청은 계정 기준으로 센다")
    void authenticatedRequestsUseUserKey() throws Exception {
        login("member@example.com");
        when(valueOperations.increment(eq("ratelimit:user:member@example.com"))).thenReturn(1L);

        assertThat(interceptor.preHandle(request("/community/posts"), response, new Object())).isTrue();
        assertThat(response.getHeader("X-RateLimit-Limit")).isEqualTo("300");
    }

    @Test
    @DisplayName("미인증 요청은 IP 기준으로 센다")
    void anonymousRequestsUseIpKey() throws Exception {
        when(valueOperations.increment(startsWith("ratelimit:ip:"))).thenReturn(1L);

        assertThat(interceptor.preHandle(request("/community/posts"), response, new Object())).isTrue();
        assertThat(response.getHeader("X-RateLimit-Limit")).isEqualTo("120");
    }

    @Test
    @DisplayName("첫 요청에서만 만료 시간을 건다")
    void setsExpiryOnlyOnFirstHit() throws Exception {
        when(valueOperations.increment(anyString())).thenReturn(1L);
        interceptor.preHandle(request("/community/posts"), response, new Object());

        org.mockito.Mockito.verify(redisTemplate).expire(anyString(), any(Duration.class));
    }
}
