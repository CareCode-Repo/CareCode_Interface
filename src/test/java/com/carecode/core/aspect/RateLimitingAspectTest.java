package com.carecode.core.aspect;

import com.carecode.core.annotation.RateLimit;
import com.carecode.core.exception.RateLimitExceededException;
import com.carecode.core.util.ClientIpResolver;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.annotation.Annotation;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@code @RateLimit} 을 처리하는 AOP 의 계약.
 *
 * <p>로그인·가입·인증코드 발송/검증·챗봇이 이 어노테이션에 기대고 있다.
 * <ul>
 *   <li>Redis 쪽에서 무슨 일이 나든 요청은 통과한다(fail-open). 예전에는 DataAccessException 만
 *       잡아서 그 밖의 실패가 500 이 됐다. 회원가입 통합 테스트가 이것 때문에 500 으로 떨어져 드러났다.</li>
 *   <li>로그인한 요청은 계정 기준, 비로그인은 IP 기준으로 센다.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("RateLimitingAspect")
class RateLimitingAspectTest {

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;
    @Mock private ProceedingJoinPoint joinPoint;
    @Mock private Signature signature;

    private RateLimitingAspect aspect;

    private static RateLimit limit(int requests) {
        return new RateLimit() {
            @Override public Class<? extends Annotation> annotationType() { return RateLimit.class; }
            @Override public int requests() { return requests; }
            @Override public int windowSeconds() { return 60; }
            @Override public String message() { return "too many"; }
            @Override public boolean perUser() { return true; }
        };
    }

    @BeforeEach
    void setUp() throws Throwable {
        aspect = new RateLimitingAspect(redisTemplate, new ClientIpResolver(false));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn("AuthController.login(..)");
        when(joinPoint.proceed()).thenReturn("ok");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.5");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("한도 안이면 통과한다")
    void allowsWithinLimit() throws Throwable {
        when(valueOperations.increment(anyString())).thenReturn(1L);

        assertThat(aspect.rateLimit(joinPoint, limit(5))).isEqualTo("ok");
    }

    @Test
    @DisplayName("한도를 넘으면 막고 대상 메서드를 실행하지 않는다")
    void blocksOverLimit() throws Throwable {
        when(valueOperations.increment(anyString())).thenReturn(6L);

        assertThatThrownBy(() -> aspect.rateLimit(joinPoint, limit(5)))
                .isInstanceOf(RateLimitExceededException.class);
        verify(joinPoint, never()).proceed();
    }

    @Test
    @DisplayName("Redis 연결이 끊겨도 통과한다")
    void failsOpenOnRedisOutage() throws Throwable {
        when(valueOperations.increment(anyString())).thenThrow(new RedisConnectionFailureException("down"));

        assertThat(aspect.rateLimit(joinPoint, limit(5))).isEqualTo("ok");
    }

    @Test
    @DisplayName("DataAccessException 이 아닌 실패에도 통과한다")
    void failsOpenOnAnyRuntimeFailure() throws Throwable {
        // 예전에는 여기서 NPE 가 그대로 올라가 500 이었다.
        when(redisTemplate.opsForValue()).thenReturn(null);

        assertThat(aspect.rateLimit(joinPoint, limit(5))).isEqualTo("ok");
    }

    @Test
    @DisplayName("대상 메서드가 던진 예외는 삼키지 않는다")
    void doesNotSwallowTargetExceptions() throws Throwable {
        // fail-open 범위를 넓히면서 대상 메서드의 예외까지 먹으면 안 된다.
        when(valueOperations.increment(anyString())).thenReturn(1L);
        when(joinPoint.proceed()).thenThrow(new IllegalArgumentException("비즈니스 오류"));

        assertThatThrownBy(() -> aspect.rateLimit(joinPoint, limit(5)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("비즈니스 오류");
    }

    @Test
    @DisplayName("비로그인은 IP 기준, 로그인은 계정 기준으로 센다")
    void keysByIpOrPrincipal() throws Throwable {
        when(valueOperations.increment(anyString())).thenReturn(1L);

        aspect.rateLimit(joinPoint, limit(5));
        verify(valueOperations).increment(eq("ratelimit:method:AuthController.login(..):ip:203.0.113.5"));

        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "me@example.com", null, List.of(new SimpleGrantedAuthority("ROLE_PARENT"))));
        aspect.rateLimit(joinPoint, limit(5));
        verify(valueOperations).increment(startsWith("ratelimit:method:AuthController.login(..):user:me@example.com"));
    }
}
