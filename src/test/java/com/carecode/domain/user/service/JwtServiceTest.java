package com.carecode.domain.user.service;

import com.carecode.core.exception.BusinessException;
import com.carecode.domain.user.dto.response.TokenDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * JWT 토큰 종류 분리에 대한 회귀 테스트.
 *
 * <p>과거에는 Access/Refresh 를 구분하는 클레임이 없어서
 * 30일짜리 Refresh Token 으로 보호된 API 에 접근할 수 있었다.
 */
@DisplayName("JwtService - 토큰 종류 검증")
class JwtServiceTest {

    private static final String SECRET = "testJwtSecretKeyForUnitTestsMustBeAtLeast256BitsLong0123456789";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", SECRET);
        ReflectionTestUtils.setField(jwtService, "issuer", "carecode-test");
        ReflectionTestUtils.setField(jwtService, "accessTokenExpiration", 3_600_000L);
        ReflectionTestUtils.setField(jwtService, "refreshTokenExpiration", 2_592_000_000L);
    }

    @Nested
    @DisplayName("Access Token 검증")
    class AccessTokenValidation {

        @Test
        @DisplayName("정상 발급된 Access Token 은 통과한다")
        void acceptsAccessToken() {
            String accessToken = jwtService.generateAccessToken("u-1", "user@example.com", "PARENT");

            assertThat(jwtService.validateAccessToken(accessToken)).isTrue();
            assertThat(jwtService.getTokenType(accessToken)).isEqualTo(JwtService.TOKEN_TYPE_ACCESS);
        }

        @Test
        @DisplayName("Refresh Token 으로는 API 인증을 통과할 수 없다")
        void rejectsRefreshTokenAsAccessToken() {
            String refreshToken = jwtService.generateRefreshToken("u-1", "user@example.com");

            assertThat(jwtService.validateAccessToken(refreshToken)).isFalse();
        }
    }

    @Nested
    @DisplayName("Refresh Token 검증")
    class RefreshTokenValidation {

        @Test
        @DisplayName("정상 발급된 Refresh Token 으로 토큰을 갱신할 수 있다")
        void refreshesWithRefreshToken() {
            String refreshToken = jwtService.generateRefreshToken("u-1", "user@example.com");

            TokenDto refreshed = jwtService.refreshTokens(refreshToken);

            assertThat(refreshed.getAccessToken()).isNotBlank();
            assertThat(jwtService.validateAccessToken(refreshed.getAccessToken())).isTrue();
            assertThat(jwtService.validateRefreshToken(refreshed.getRefreshToken())).isTrue();
        }

        @Test
        @DisplayName("Access Token 을 Refresh 엔드포인트에 재사용할 수 없다")
        void rejectsAccessTokenOnRefresh() {
            String accessToken = jwtService.generateAccessToken("u-1", "user@example.com", "PARENT");

            assertThat(jwtService.validateRefreshToken(accessToken)).isFalse();
            assertThatThrownBy(() -> jwtService.refreshTokens(accessToken))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Test
    @DisplayName("다른 서명 키로 만든 토큰은 거부한다")
    void rejectsTokenSignedWithAnotherKey() {
        JwtService attacker = new JwtService();
        ReflectionTestUtils.setField(attacker, "secret",
                "completelyDifferentSecretKeyThatIsAlsoAtLeast256BitsLong0123456789");
        ReflectionTestUtils.setField(attacker, "issuer", "carecode-test");
        ReflectionTestUtils.setField(attacker, "accessTokenExpiration", 3_600_000L);
        ReflectionTestUtils.setField(attacker, "refreshTokenExpiration", 2_592_000_000L);

        String forged = attacker.generateAccessToken("u-1", "attacker@example.com", "ADMIN");

        assertThat(jwtService.validateAccessToken(forged)).isFalse();
    }

    @Test
    @DisplayName("발급자(issuer)가 다른 토큰은 거부한다")
    void rejectsTokenWithDifferentIssuer() {
        JwtService other = new JwtService();
        ReflectionTestUtils.setField(other, "secret", SECRET);
        ReflectionTestUtils.setField(other, "issuer", "someone-else");
        ReflectionTestUtils.setField(other, "accessTokenExpiration", 3_600_000L);
        ReflectionTestUtils.setField(other, "refreshTokenExpiration", 2_592_000_000L);

        String foreignToken = other.generateAccessToken("u-1", "user@example.com", "PARENT");

        assertThat(jwtService.validateAccessToken(foreignToken)).isFalse();
    }

    @Test
    @DisplayName("만료된 토큰은 거부한다")
    void rejectsExpiredToken() {
        ReflectionTestUtils.setField(jwtService, "accessTokenExpiration", -1_000L);
        String expired = jwtService.generateAccessToken("u-1", "user@example.com", "PARENT");

        assertThat(jwtService.validateAccessToken(expired)).isFalse();
    }
}
