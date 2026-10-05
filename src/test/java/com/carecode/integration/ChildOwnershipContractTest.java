package com.carecode.integration;

import com.carecode.CareCodeApplication;
import com.carecode.core.ops.OperationalAlerter;
import com.carecode.domain.user.entity.Child;
import com.carecode.domain.user.entity.User;
import com.carecode.domain.user.entity.UserRole;
import com.carecode.domain.user.repository.ChildRepository;
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
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * "이 아이가 내 아이인가" 에 대한 답은 경로마다 같아야 한다.
 *
 * <p>이 검증이 세 군데에 각자 쓰여 있었고, 답이 셋으로 갈렸다.
 *
 * <ul>
 *   <li>{@code /children/…} — 404 ({@code ChildNotFoundException}). 맞는 동작이다.</li>
 *   <li>{@code HealthService.assertChildOwnedByUser} — 403. <b>자녀가 존재한다는 사실을 알려준다.</b>
 *       ID 를 1부터 훑으면 "이 번호는 누군가의 자녀" 라는 목록을 만들 수 있다.</li>
 *   <li>{@code /health/…/schedule} — <b>500.</b> 위 403 이 {@code catch (Exception e)} 에 걸려
 *       {@code CareServiceException} 으로 덮이면서 서버 오류가 됐다. 권한 거부가 장애로 집계되고,
 *       클라이언트는 "내가 잘못 요청했다" 를 알 수 없다. (Slack 알림까지 가지는 않았다 —
 *       알림은 최후 핸들러에서만 울리고 {@code CareServiceException} 은 자기 핸들러가 있다.)</li>
 * </ul>
 *
 * <p>셋 중 404 가 맞다. 없는 자녀와 남의 자녀를 구분해 주지 않는 쪽이다.
 */
@SpringBootTest(
        classes = CareCodeApplication.class,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration",
                "spring.cache.type=none",
                "spring.datasource.url=jdbc:h2:mem:carecode_child_owner;MODE=MySQL;DB_CLOSE_DELAY=-1",
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
@DisplayName("자녀 소유권 계약")
class ChildOwnershipContractTest {

    @MockBean RedisConnectionFactory redisConnectionFactory;
    @MockBean StringRedisTemplate stringRedisTemplate;
    @MockBean JavaMailSender javaMailSender;
    @MockBean OperationalAlerter alerter;

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired UserRepository userRepository;
    @Autowired ChildRepository childRepository;

    private User stranger;
    private Child child;

    @BeforeEach
    void setUp() {
        User parent = saveUser();
        stranger = saveUser();
        child = childRepository.save(Child.builder()
                .user(parent)
                .name("아이")
                .birthDate(LocalDate.now().minusMonths(13))
                .gender("FEMALE")
                .createdAt(LocalDateTime.now())
                .build());
    }

    /**
     * 경로에 {@code %d} 가 있으면 자녀 ID 가 들어간다.
     *
     * <p>{@code /health/…} 는 자녀 ID 를 쿼리 파라미터로 받는다 — 같은 질문이지만 모양이 달라
     * 서로 다른 검증을 쓰게 된 원인이기도 하다.
     */
    @ParameterizedTest(name = "{0} 은 남의 아이에게 404 다")
    @ValueSource(strings = {
            "/children/%d",
            "/children/%d/timeline",
            "/children/%d/vaccinations",
            "/children/%d/growth",
            "/health/vaccines/schedule?childId=%d",
            "/health/checkups/schedule?childId=%d"
    })
    void otherParentAlwaysGetsNotFound(String pathTemplate) throws Exception {
        String path = pathTemplate.formatted(child.getId());

        int status = mockMvc.perform(get(path).header("Authorization", "Bearer " + token(stranger)))
                .andReturn().getResponse().getStatus();

        assertThat(status)
                .as("%s — 403 이면 자녀의 존재를 알려주고, 500 이면 권한 거부가 장애로 집계된다", path)
                .isEqualTo(404);
    }

    /**
     * 남의 아이 접근은 공격이거나 실수다. 어느 쪽도 운영 알림을 울릴 일이 아니다.
     *
     * <p>지금은 울리지 않는다({@code CareServiceException} 에 전용 핸들러가 있어 최후 핸들러까지
     * 가지 않는다). 다만 이 경로가 최후 핸들러로 떨어지게 바뀌면 그때부터 울리므로 여기서 막는다.
     */
    @Test
    @DisplayName("남의 아이 접근은 운영 알림을 울리지 않는다")
    void deniedAccessDoesNotPage() throws Exception {
        mockMvc.perform(get("/health/vaccines/schedule?childId=" + child.getId())
                .header("Authorization", "Bearer " + token(stranger))).andReturn();

        verify(alerter, never()).alert(anyString(), anyString(), anyString());
    }

    /** 없는 자녀와 남의 자녀의 응답이 같아야 ID 를 훑어도 아무것도 알 수 없다. */
    @Test
    @DisplayName("없는 아이와 남의 아이의 응답이 구분되지 않는다")
    void missingAndForeignChildLookIdentical() throws Exception {
        int foreign = mockMvc.perform(get("/children/{id}", child.getId())
                .header("Authorization", "Bearer " + token(stranger))).andReturn().getResponse().getStatus();
        int missing = mockMvc.perform(get("/children/{id}", 99_999_999L)
                .header("Authorization", "Bearer " + token(stranger))).andReturn().getResponse().getStatus();

        assertThat(foreign).isEqualTo(missing);
    }

    private String token(User user) {
        return jwtService.generateAccessToken(user.getUserId(), user.getEmail(), user.getRole().name());
    }

    private User saveUser() {
        String id = UUID.randomUUID().toString().substring(0, 8);
        return userRepository.save(User.builder()
                .userId("user_" + id)
                .email(id + "@example.com")
                .password("{noop}unused")
                .name("보호자" + id)
                .role(UserRole.PARENT)
                .isActive(true)
                .emailVerified(true)
                .registrationCompleted(true)
                .createdAt(LocalDateTime.now())
                .build());
    }
}
