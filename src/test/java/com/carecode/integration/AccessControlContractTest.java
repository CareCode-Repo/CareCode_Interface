package com.carecode.integration;

import com.carecode.CareCodeApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

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
            "/health/hospitals/statistics",
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
            "/auth/user/profile"
    })
    void protectedPathsRequireLogin(String path) throws Exception {
        MvcResult result = mockMvc.perform(get(path)).andReturn();

        assertThat(result.getResponse().getStatus())
                .as("%s 는 인증을 요구해야 한다", path)
                .isEqualTo(401);
    }
}
