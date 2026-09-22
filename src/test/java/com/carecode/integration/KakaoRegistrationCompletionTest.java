package com.carecode.integration;

import com.carecode.CareCodeApplication;
import com.carecode.domain.user.entity.User;
import com.carecode.domain.user.entity.UserRole;
import com.carecode.domain.user.repository.UserRepository;
import com.carecode.domain.user.service.JwtService;
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
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * 카카오 신규 가입 완료 흐름.
 *
 * <p>여기에는 결함이 두 개 겹쳐 있었고, <b>하나만 고치면 더 나빠지는</b> 구조였다.
 *
 * <ol>
 *   <li><b>흐름이 동작하지 않았다.</b> 카카오 로그인은 가입 미완료 사용자에게도 토큰을 주고,
 *       프런트는 그 토큰으로 이 엔드포인트를 부른다. 그런데 JWT 필터가 {@code /auth/kakao}
 *       로 시작하는 경로를 통째로 건너뛰어 토큰이 해석되지 않았고, 컨트롤러가 현재 사용자를
 *       찾다가 401 을 냈다. 즉 카카오로 처음 들어온 사람은 누구도 가입을 끝낼 수 없었다.</li>
 *   <li><b>역할을 클라이언트가 정했다.</b> 요청 본문의 {@code role} 을 그대로 저장해서,
 *       {@code "ADMIN"} 을 보내면 그 자리에서 관리자가 된다. 이메일 가입 경로(#82)와 같은 결함이다.</li>
 * </ol>
 *
 * <p>1번만 고치면 2번이 그대로 열린다. 지금까지 2번이 드러나지 않은 건 1번 덕분이었다.
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
                "spring.datasource.url=jdbc:h2:mem:carecode_kakao;MODE=MySQL;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.flyway.enabled=false",
                "jwt.secret=testJwtSecretKeyForKakaoRegistrationMustBe256BitsLong0123456789",
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
@DisplayName("카카오 가입 완료")
class KakaoRegistrationCompletionTest {

    private static final String EMAIL = "kakao-newbie@example.com";

    @MockBean RedisConnectionFactory redisConnectionFactory;
    @MockBean StringRedisTemplate stringRedisTemplate;
    @MockBean JavaMailSender javaMailSender;

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired UserRepository userRepository;

    private String accessToken;

    @BeforeEach
    void setUp() {
        userRepository.findByEmail(EMAIL).ifPresent(userRepository::delete);

        // 카카오 로그인 직후의 상태: 계정은 있지만 가입 절차를 끝내지 않았다.
        User user = userRepository.save(User.builder()
                .email(EMAIL)
                .name("카카오_12345")
                .role(UserRole.PARENT)
                .provider("kakao")
                .providerId("12345")
                .isActive(true)
                .emailVerified(true)
                .registrationCompleted(false)
                .createdAt(LocalDateTime.now())
                .build());

        accessToken = jwtService.generateAccessToken(user.getUserId(), EMAIL, "PARENT", user.getName());
    }

    private MvcResult complete(String role) throws Exception {
        return mockMvc.perform(post("/auth/kakao/complete-registration")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"맘편한\",\"role\":\"" + role + "\"}"))
                .andReturn();
    }

    @Test
    @DisplayName("카카오 로그인으로 받은 토큰으로 가입을 끝낼 수 있다")
    void newKakaoUserCanCompleteRegistration() throws Exception {
        MvcResult result = complete("PARENT");

        assertThat(result.getResponse().getStatus())
                .as("JWT 필터가 이 경로를 건너뛰면 토큰이 해석되지 않아 401 이 된다")
                .isEqualTo(200);

        User saved = userRepository.findByEmail(EMAIL).orElseThrow();
        assertThat(saved.getRegistrationCompleted()).isTrue();
        assertThat(saved.getName()).isEqualTo("맘편한");
    }

    @Test
    @DisplayName("보육사로도 가입할 수 있다")
    void caregiverIsSelfSelectable() throws Exception {
        assertThat(complete("CAREGIVER").getResponse().getStatus()).isEqualTo(200);
        assertThat(userRepository.findByEmail(EMAIL).orElseThrow().getRole()).isEqualTo(UserRole.CAREGIVER);
    }

    @ParameterizedTest(name = "role={0} 으로는 가입을 끝낼 수 없다")
    @ValueSource(strings = {"ADMIN", "GUEST"})
    @DisplayName("스스로 고를 수 없는 역할은 거부한다")
    void privilegedRoleIsRejected(String role) throws Exception {
        MvcResult result = complete(role);

        assertThat(result.getResponse().getStatus()).isEqualTo(400);

        User saved = userRepository.findByEmail(EMAIL).orElseThrow();
        assertThat(saved.getRole())
                .as("요청의 role 이 그대로 저장되면 그 자리에서 관리자가 된다")
                .isEqualTo(UserRole.PARENT);
        assertThat(saved.getRegistrationCompleted()).isFalse();
    }

    @Test
    @DisplayName("토큰 없이는 가입을 끝낼 수 없다")
    void requiresAuthentication() throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/kakao/complete-registration")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"맘편한\",\"role\":\"PARENT\"}"))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("카카오 로그인 자체는 여전히 토큰 없이 호출된다")
    void kakaoLoginStaysPublic() throws Exception {
        // 필터 예외를 좁히면서 로그인까지 막으면 안 된다.
        MvcResult result = mockMvc.perform(post("/auth/kakao/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isNotIn(401, 403);
    }
}
