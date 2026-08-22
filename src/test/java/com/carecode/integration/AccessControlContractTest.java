package com.carecode.integration;

import com.carecode.CareCodeApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * 어떤 경로가 로그인 없이 열려 있고 어떤 경로가 막혀 있어야 하는지를 코드로 고정한다.
 *
 * <p>SecurityConfig 는 선언 순서에 따라 앞선 규칙이 뒤를 덮는다. 실제로 병원 공개 규칙이
 * 존재하지 않는 {@code /hospitals/**} 에 걸려 있고 앞선 {@code /health/**} 가 전부 잡아
 * 병원 조회가 통째로 로그인 필수였는데, 규칙 자체는 멀쩡해 보여서 아무도 눈치채지 못했다.
 * 클래스 레벨 {@code @PreAuthorize} 가 URL 규칙을 덮는 경우도 마찬가지다.
 *
 * <p>그래서 규칙을 읽는 대신 실제 응답 코드를 확인한다.
 */
@SpringBootTest(
        classes = CareCodeApplication.class,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration",
                "spring.cache.type=none",
                "spring.batch.job.enabled=false",
                "spring.datasource.url=jdbc:h2:mem:carecode_acl;MODE=MySQL;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.flyway.enabled=false",
                "jwt.secret=testJwtSecretKeyForAccessControlTestMustBe256BitsLong0123456789",
                "springdoc.api-docs.enabled=false",
                "springdoc.swagger-ui.enabled=false",
                "public.data.api.key=dummy",
                "KAKAO_CLIENT_ID=dummy-kakao-client",
                "KAKAO_CLIENT_SECRET=dummy-kakao-secret",
                "MAIL_USERNAME=dummy",
                "MAIL_PASSWORD=dummy"
        }
)
@AutoConfigureMockMvc
@DisplayName("접근제어 계약")
class AccessControlContractTest {

    @MockBean
    RedisConnectionFactory redisConnectionFactory;

    @MockBean
    StringRedisTemplate stringRedisTemplate;

    @MockBean
    JavaMailSender javaMailSender;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    com.carecode.domain.user.service.JwtService jwtService;

    /**
     * 로그인 전에도 보여야 하는 경로.
     *
     * <p>여기서 확인하는 건 인가지 응답 내용이 아니다. 데이터가 없어 404 가 나올 수는 있어도
     * 인증을 요구해서는 안 된다.
     */
    @ParameterizedTest(name = "{0} 은 로그인 없이 열려 있다")
    @ValueSource(strings = {
            "/actuator/health",
            // 동의하기 전에 읽어야 하는 문서
            "/legal/privacy-policy",
            "/legal/terms",
            "/legal/version",
            // 둘러보기 단계에서 보여줘야 가입 전환이 생긴다
            "/policies",
            "/policies/categories",
            "/policies/statistics",
            "/facilities",
            "/facilities/popular",
            "/facilities/statistics",
            "/health/hospitals",
            "/health/hospitals/popular",
            "/community/posts",
            "/community/tags"
    })
    void publicPathsDoNotRequireLogin(String path) throws Exception {
        MvcResult result = mockMvc.perform(get(path)).andReturn();

        assertThat(result.getResponse().getStatus())
                .as("%s 는 비로그인 접근이 가능해야 한다", path)
                .isNotIn(401, 403);
    }

    /** 남의 개인정보가 걸린 경로. 뚫리면 그대로 사고다. */
    @ParameterizedTest(name = "{0} 은 로그인이 필요하다")
    @ValueSource(strings = {
            "/policies/recommendations",
            "/policies/missed-benefits",
            "/policies/regional-comparison",
            "/policies/bookmarks",
            "/health/records/user/1",
            "/health/statistics",
            // 좋아요 "여부" 는 내 상태라 공개 조회와 구분해야 한다
            "/health/hospitals/1/like-status",
            "/notifications",
            "/auth/user/profile",
            // 본인 계정 API. 로그인 없이 열리면 남의 프로필이 그대로 노출된다
            "/users/profile",
            "/users/me",
            // 관리 API 는 비로그인부터 막힌다
            "/api/admin/users",
            "/api/admin/users/statistics"
    })
    void protectedPathsRequireLogin(String path) throws Exception {
        MvcResult result = mockMvc.perform(get(path)).andReturn();

        assertThat(result.getResponse().getStatus())
                .as("%s 는 인증을 요구해야 한다", path)
                .isEqualTo(401);
    }

    /**
     * 가입 흐름에서 로그인 전에 호출되는 POST 경로.
     *
     * <p>이 규칙은 오랫동안 존재하지 않는 {@code /users/send-code} 를 가리키고 있었다.
     * 실제 매핑인 {@code /auth/send-code} 는 화이트리스트에 없어 {@code anyRequest().authenticated()}
     * 에 걸렸고, 그 결과 "가입하려면 먼저 로그인해야 하는" 상태였다.
     * 메일로 받은 인증 링크({@code GET /auth/verify}) 역시 같은 이유로 401 이었다.
     */
    @ParameterizedTest(name = "{0} 은 가입 전에 호출할 수 있어야 한다")
    @ValueSource(strings = {"/auth/send-code", "/auth/verify-code"})
    void signupFlowPostPathsDoNotRequireLogin(String path) throws Exception {
        MvcResult result = mockMvc.perform(post(path)).andReturn();

        // 파라미터가 없어 400 이 날 수는 있어도, 인증을 요구해서는 안 된다.
        assertThat(result.getResponse().getStatus())
                .as("%s 는 비로그인 접근이 가능해야 한다", path)
                .isNotIn(401, 403);
    }

    @Test
    @DisplayName("이메일 인증 링크는 로그인 없이 열린다")
    void emailVerificationLinkIsPublic() throws Exception {
        MvcResult result = mockMvc.perform(get("/auth/verify").param("token", "dummy-token")).andReturn();

        assertThat(result.getResponse().getStatus()).isNotIn(401, 403);
    }

    /**
     * 권한 상승 회귀 방지.
     *
     * <p>{@code PUT /users/{id}/role} 은 클래스 제약이 {@code isAuthenticated()} 뿐이어서,
     * 가입만 하면 누구나 자신을 ADMIN 으로 올리고 {@code /api/admin/**} 전체를 열 수 있었다.
     * 해당 매핑은 삭제했고, 역할 변경은 관리자 전용 경로로만 남겼다.
     */
    @ParameterizedTest(name = "{0} 매핑은 더 이상 존재하지 않는다")
    @ValueSource(strings = {
            "/users/1/role",
            "/users/1/activate",
            "/users/1/reactivate"
    })
    @WithMockUser(username = "attacker@example.com", roles = "PARENT")
    void privilegedMappingsRemovedFromUserApi(String path) throws Exception {
        MvcResult result = mockMvc.perform(
                put(path).contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"ADMIN\"}")).andReturn();

        assertThat(result.getResponse().getStatus())
                .as("%s 는 매핑이 없어야 한다 (404/405)", path)
                .isIn(404, 405);
    }

    /** 사용자 목록·검색은 전체 회원 개인정보다. 로그인만 했다고 열리면 안 된다. */
    @ParameterizedTest(name = "{0} 은 일반 회원에게 403 이다")
    @ValueSource(strings = {
            "/api/admin/users",
            "/api/admin/users/statistics",
            "/api/admin/users/search?keyword=a",
            "/api/admin/users/active",
            "/api/admin/users/verified",
            "/api/admin/users/by-region/서울"
    })
    @WithMockUser(username = "member@example.com", roles = "PARENT")
    void adminQueriesRejectNonAdmin(String path) throws Exception {
        MvcResult result = mockMvc.perform(get(path)).andReturn();

        assertThat(result.getResponse().getStatus())
                .as("%s 는 ADMIN 이 아니면 막혀야 한다", path)
                .isEqualTo(403);
    }

    /**
     * 토큰 종류 혼동 회귀 방지.
     *
     * <p>{@code /auth/refresh} 는 서명·만료만 보는 {@code validateToken} 을 쓰고 있었다.
     * 그 검증은 Access Token 도 통과시키고, 서버 세션 저장소를 쓰지 않는 기본 설정
     * ({@code jwt.refresh-token.store=none}) 에서는 뒤따르는 등록 여부 확인도 항상 참이라,
     * 탈취한 1시간짜리 Access Token 을 30일짜리 Refresh Token 으로 바꿀 수 있었다.
     */
    @Test
    @DisplayName("Access Token 으로는 토큰을 갱신할 수 없다")
    void accessTokenCannotBeUsedToRefresh() throws Exception {
        String accessToken = jwtService.generateAccessToken("u-1", "victim@example.com", "PARENT");

        MvcResult result = mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + accessToken + "\"}"))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("Refresh Token 은 갱신 경로에서 토큰 종류 검증을 통과한다")
    void refreshTokenPassesTypeCheck() throws Exception {
        String refreshToken = jwtService.generateRefreshToken("u-1", "victim@example.com");

        MvcResult result = mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andReturn();

        // 해당 사용자가 DB 에 없으므로 최종 응답은 404 다. 중요한 건 "토큰 종류 때문에 401" 이
        // 아니라는 점이다. 여기까지 왔다는 것은 종류 검증을 통과했다는 뜻이다.
        assertThat(result.getResponse().getStatus())
                .as("정상 Refresh Token 이 종류 검증에서 막히면 안 된다")
                .isNotEqualTo(401);
    }

    @Test
    @DisplayName("일반 회원은 관리자 경로로도 역할을 바꿀 수 없다")
    void nonAdminCannotEscalateThroughAdminPath() throws Exception {
        MvcResult result = mockMvc.perform(put("/api/admin/users/1/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"ADMIN\"}")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                                .user("member@example.com").roles("PARENT")))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(403);
    }
}
