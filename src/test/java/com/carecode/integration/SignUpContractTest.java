package com.carecode.integration;

import com.carecode.CareCodeApplication;
import com.carecode.domain.user.entity.User;
import com.carecode.domain.user.entity.UserRole;
import com.carecode.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * 회원가입 요청 계약 — 실제 JSON 으로 확인한다.
 *
 * <p>예전에는 응답 DTO 인 UserDto 를 요청으로 받았다. 두 가지 문제가 있었다.
 * <ul>
 *   <li>서버가 정할 값(role, provider, emailVerified …)이 요청 계약에 노출돼 있었다(#82).</li>
 *   <li>UserDto 에 검증 어노테이션이 없어 {@code @Valid} 가 아무 일도 하지 않았다.
 *       빈 이메일·형식이 틀린 이메일·한 글자 비밀번호로 가입됐고, 이름이 없으면 DB 제약에서 500 이었다.</li>
 * </ul>
 */
@SpringBootTest(
        classes = CareCodeApplication.class,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration",
                "spring.cache.type=none",
                "spring.datasource.url=jdbc:h2:mem:carecode_signup;MODE=MySQL;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.flyway.enabled=false",
                "jwt.secret=testJwtSecretKeyForSignUpContractMustBe256BitsLong0123456789ab",
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
@DisplayName("회원가입 요청 계약")
class SignUpContractTest {

    private static final String EMAIL = "signup-contract@example.com";

    @MockBean RedisConnectionFactory redisConnectionFactory;
    @MockBean StringRedisTemplate stringRedisTemplate;
    @MockBean JavaMailSender javaMailSender;

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;

    @BeforeEach
    void clean() {
        userRepository.findByEmail(EMAIL).ifPresent(userRepository::delete);
    }

    private int register(String json) throws Exception {
        return mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(json))
                .andReturn().getResponse().getStatus();
    }

    @Test
    @DisplayName("요청에 role·provider·emailVerified 를 넣어도 서버가 정한 값으로 저장된다")
    void serverControlledFieldsCannotBeSetByClient() throws Exception {
        int status = register("""
                {"email":"%s","password":"secret123","name":"공격자",
                 "role":"ADMIN","provider":"kakao","providerId":"1","emailVerified":true,"isActive":true}
                """.formatted(EMAIL));

        assertThat(status).isEqualTo(200);
        User saved = userRepository.findByEmail(EMAIL).orElseThrow();
        assertThat(saved.getRole()).isEqualTo(UserRole.PARENT);
        assertThat(saved.getProvider()).isNull();
        assertThat(saved.getProviderId()).isNull();
        assertThat(saved.getEmailVerified()).isFalse();
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
            // 빈 이메일
            "{\"email\":\"\",\"password\":\"secret123\",\"name\":\"이름\"}",
            // 형식이 틀린 이메일
            "{\"email\":\"not-an-email\",\"password\":\"secret123\",\"name\":\"이름\"}",
            // 짧은 비밀번호
            "{\"email\":\"signup-contract@example.com\",\"password\":\"1\",\"name\":\"이름\"}",
            // 이름 없음 — 예전에는 DB NOT NULL 제약에서 500 이었다
            "{\"email\":\"signup-contract@example.com\",\"password\":\"secret123\"}",
            // 성별 값 오류 — 예전에는 Gender.valueOf 에서 500 이었다
            "{\"email\":\"signup-contract@example.com\",\"password\":\"secret123\",\"name\":\"이름\",\"gender\":\"X\"}",
            // 날짜 형식 오류 — 예전에는 역직렬화 실패가 500 이었다
            "{\"email\":\"signup-contract@example.com\",\"password\":\"secret123\",\"name\":\"이름\",\"birthDate\":\"어제\"}"
    })
    @DisplayName("잘못된 가입 요청은 400 이고 계정이 생기지 않는다")
    void invalidRequestsAreRejected(String json) throws Exception {
        assertThat(register(json)).isEqualTo(400);
        assertThat(userRepository.findByEmail(EMAIL)).isEmpty();
    }
}
