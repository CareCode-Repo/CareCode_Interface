package com.carecode.integration;

import com.carecode.CareCodeApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.web.FilterChainProxy;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Docker 없이도 도는 컨텍스트 로딩 테스트.
 *
 * <p>보안 필터 체인, 인터셉터, 캐시 설정 등 빈 구성이 깨지면 여기서 바로 잡힌다.
 * MariaDB Testcontainers 통합 테스트는 Docker 가 없으면 스킵되므로 그 공백을 메운다.
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
                "spring.datasource.url=jdbc:h2:mem:carecode;MODE=MySQL;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.flyway.enabled=false",
                "jwt.secret=testJwtSecretKeyForContextLoadTestMustBe256BitsLong0123456789",
                "springdoc.api-docs.enabled=false",
                "springdoc.swagger-ui.enabled=false",
                "public.data.api.key=dummy",
                "KAKAO_CLIENT_ID=dummy-kakao-client",
                "KAKAO_CLIENT_SECRET=dummy-kakao-secret",
                "MAIL_USERNAME=dummy",
                "MAIL_PASSWORD=dummy"
        }
)
@DisplayName("애플리케이션 컨텍스트 로딩")
class ApplicationContextLoadTest {

    @MockBean
    private RedisConnectionFactory redisConnectionFactory;

    @MockBean
    private StringRedisTemplate stringRedisTemplate;

    // 메일 자동설정을 제외했으므로 대체 빈을 넣어준다 (실제 SMTP 연결은 하지 않는다).
    @MockBean
    private JavaMailSender javaMailSender;

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    @DisplayName("컨텍스트가 정상적으로 기동된다")
    void contextLoads() {
        assertThat(applicationContext).isNotNull();
    }

    @Test
    @DisplayName("보안 필터 체인이 등록되어 있다")
    void securityFilterChainIsRegistered() {
        FilterChainProxy proxy = applicationContext.getBean(FilterChainProxy.class);

        assertThat(proxy.getFilterChains()).isNotEmpty();
    }

    @Test
    @DisplayName("Rate limit 인터셉터가 빈으로 등록되어 있다")
    void rateLimitInterceptorIsRegistered() {
        assertThat(applicationContext.getBeansOfType(com.carecode.core.RateLimitInterceptor.class))
                .isNotEmpty();
        assertThat(applicationContext.getBeansOfType(com.carecode.core.config.WebMvcConfig.class))
                .isNotEmpty();
    }
}
