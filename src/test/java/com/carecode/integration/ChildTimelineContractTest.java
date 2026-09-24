package com.carecode.integration;

import com.carecode.CareCodeApplication;
import com.carecode.domain.health.entity.VaccinationSchedule;
import com.carecode.domain.health.entity.VaccineType;
import com.carecode.domain.health.repository.VaccinationScheduleRepository;
import com.carecode.domain.user.entity.Child;
import com.carecode.domain.user.entity.User;
import com.carecode.domain.user.entity.UserRole;
import com.carecode.domain.user.repository.ChildRepository;
import com.carecode.domain.user.repository.UserRepository;
import com.carecode.domain.user.service.JwtService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/** 타임라인은 아이 개인정보다. 소유권과 응답 모양을 실제 필터 체인으로 고정한다. */
@SpringBootTest(
        classes = CareCodeApplication.class,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration",
                "spring.cache.type=none",
                "spring.datasource.url=jdbc:h2:mem:carecode_child_timeline;MODE=MySQL;DB_CLOSE_DELAY=-1",
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
@DisplayName("아이 타임라인 계약")
class ChildTimelineContractTest {

    @MockBean RedisConnectionFactory redisConnectionFactory;
    @MockBean StringRedisTemplate stringRedisTemplate;
    @MockBean JavaMailSender javaMailSender;

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired JwtService jwtService;
    @Autowired UserRepository userRepository;
    @Autowired ChildRepository childRepository;
    @Autowired VaccinationScheduleRepository vaccinationScheduleRepository;

    private User parent;
    private User stranger;
    private Child child;

    @BeforeEach
    void setUp() {
        parent = saveUser();
        stranger = saveUser();
        child = childRepository.save(Child.builder()
                .user(parent)
                .name("아이")
                .birthDate(LocalDate.now().minusMonths(13))
                .gender("FEMALE")
                .createdAt(LocalDateTime.now())
                .build());
    }

    @Test
    @DisplayName("로그인 없이는 볼 수 없다")
    void requiresLogin() throws Exception {
        assertThat(mockMvc.perform(get("/children/{id}/timeline", child.getId()))
                .andReturn().getResponse().getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("남의 아이 타임라인은 404 — 존재 여부도 알려주지 않는다")
    void otherParentCannotSee() throws Exception {
        assertThat(mockMvc.perform(get("/children/{id}/timeline", child.getId())
                        .header("Authorization", "Bearer " + token(stranger)))
                .andReturn().getResponse().getStatus()).isEqualTo(404);
    }

    @Test
    @DisplayName("접종·검진·신학기를 한 축에 날짜순으로 준다")
    void mergesSourcesInOneAxis() throws Exception {
        vaccinationScheduleRepository.save(VaccinationSchedule.builder()
                .child(child)
                .vaccineType(VaccineType.HEP_B)
                .doseNumber(3)
                .dueDate(LocalDate.now().plusMonths(2))
                .status(VaccinationSchedule.VaccinationStatus.SCHEDULED)
                .build());

        JsonNode timeline = json(mockMvc.perform(get("/children/{id}/timeline", child.getId())
                .param("months", "24")
                .header("Authorization", "Bearer " + token(parent))).andReturn());

        assertThat(timeline.path("childId").asLong()).isEqualTo(child.getId());
        assertThat(timeline.path("items").isArray()).isTrue();
        assertThat(timeline.path("items")).isNotEmpty();

        var types = timeline.path("items").findValuesAsText("type");
        assertThat(types).contains("VACCINATION", "CHECKUP", "NEW_TERM");

        // 날짜순인지 확인. 화면이 그대로 그리므로 정렬이 계약이다.
        var dates = timeline.path("items").findValuesAsText("date");
        assertThat(dates).isSorted();

        // 놓친 항목 수가 함께 온다 (배지용).
        assertThat(timeline.path("overdueCount").isNumber()).isTrue();
        assertThat(timeline.path("upcomingCount").isNumber()).isTrue();
    }

    @Test
    @DisplayName("기간을 안 주면 12개월, 상한은 36개월")
    void windowDefaultsAndCap() throws Exception {
        JsonNode defaultWindow = json(mockMvc.perform(get("/children/{id}/timeline", child.getId())
                .header("Authorization", "Bearer " + token(parent))).andReturn());
        assertThat(defaultWindow.path("to").asText()).isEqualTo(LocalDate.now().plusMonths(12).toString());

        JsonNode capped = json(mockMvc.perform(get("/children/{id}/timeline", child.getId())
                .param("months", "120")
                .header("Authorization", "Bearer " + token(parent))).andReturn());
        assertThat(capped.path("to").asText()).isEqualTo(LocalDate.now().plusMonths(36).toString());
    }

    private JsonNode json(MvcResult result) throws Exception {
        String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(result.getResponse().getStatus()).as(body).isEqualTo(200);
        return objectMapper.readTree(body);
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
