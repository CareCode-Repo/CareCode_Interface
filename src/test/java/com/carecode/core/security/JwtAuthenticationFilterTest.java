package com.carecode.core.security;

import com.carecode.domain.user.service.JwtService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * JWT 필터가 SecurityContext 를 다루는 방식에 대한 테스트.
 *
 * <p>예전 구현은 토큰이 없을 때도 {@code SecurityContextHolder.clearContext()} 를 호출했다.
 * 운영에서는 JWT 외에 인증 수단이 없어 결과가 같았지만, "자기가 세우지 않은 컨텍스트를
 * 지우는" 필터라 앞단에서 인증을 넣어주는 경로를 전부 무력화한다.
 * 실제로 접근제어 테스트가 인증을 넣어도 401 로 떨어져 드러났다.
 */
@DisplayName("JwtAuthenticationFilter - SecurityContext 취급")
class JwtAuthenticationFilterTest {

    private static final String SECRET = "testJwtSecretKeyForFilterTestsMustBeAtLeast256BitsLong0123456789";

    private JwtService jwtService;
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", SECRET);
        ReflectionTestUtils.setField(jwtService, "issuer", "carecode-test");
        ReflectionTestUtils.setField(jwtService, "accessTokenExpiration", 3_600_000L);
        ReflectionTestUtils.setField(jwtService, "refreshTokenExpiration", 2_592_000_000L);
        filter = new JwtAuthenticationFilter(jwtService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void presetAuthentication() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "preset@example.com", null, List.of(new SimpleGrantedAuthority("ROLE_PARENT"))));
    }

    private MockHttpServletRequest request(String bearer) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/community/posts");
        if (bearer != null) {
            request.addHeader("Authorization", "Bearer " + bearer);
        }
        return request;
    }

    @Test
    @DisplayName("토큰이 없으면 기존 인증 정보를 지우지 않는다")
    void keepsExistingAuthenticationWhenNoToken() throws Exception {
        presetAuthentication();
        FilterChain chain = mock(FilterChain.class);

        MockHttpServletRequest req = request(null);
        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilter(req, res, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName())
                .isEqualTo("preset@example.com");
        verify(chain).doFilter(req, res);
    }

    @Test
    @DisplayName("유효한 Access Token 이면 email 을 principal 로 인증을 세운다")
    void authenticatesWithAccessToken() throws Exception {
        String token = jwtService.generateAccessToken("u-1", "user@example.com", "PARENT");

        filter.doFilter(request(token), new MockHttpServletResponse(), mock(FilterChain.class));

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getName()).isEqualTo("user@example.com");
        assertThat(auth.getAuthorities()).extracting("authority").containsExactly("ROLE_PARENT");
    }

    @Test
    @DisplayName("Refresh Token 으로는 인증되지 않고, 앞서 있던 인증도 폐기된다")
    void refreshTokenIsRejected() throws Exception {
        presetAuthentication();
        String refreshToken = jwtService.generateRefreshToken("u-1", "user@example.com");

        filter.doFilter(request(refreshToken), new MockHttpServletResponse(), mock(FilterChain.class));

        // 토큰을 제시했는데 그게 유효하지 않다면, 그 요청은 비인증으로 처리해야 한다.
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("변조된 토큰이면 인증하지 않는다")
    void rejectsTamperedToken() throws Exception {
        filter.doFilter(request("not-a-jwt"), new MockHttpServletResponse(), mock(FilterChain.class));

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("role 클레임이 없는 토큰은 거부한다")
    void rejectsTokenWithoutRole() throws Exception {
        // role 이 없으면 "ROLE_null" 권한으로 인증되던 문제를 막는다.
        String noRoleToken = jwtService.generateAccessToken("u-1", "user@example.com", null);

        filter.doFilter(request(noRoleToken), new MockHttpServletResponse(), mock(FilterChain.class));

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("토큰 유무와 무관하게 필터 체인은 항상 이어진다")
    void alwaysContinuesChain() throws Exception {
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletRequest req = request("not-a-jwt");
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, chain);

        // 인가 판단은 SecurityConfig 가 한다. 필터가 직접 응답을 끊으면 공개 경로까지 막힌다.
        verify(chain).doFilter(req, res);
    }
}
