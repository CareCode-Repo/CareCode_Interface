package com.carecode.integration;

import com.carecode.CareCodeApplication;
import com.carecode.domain.policy.entity.Policy;
import com.carecode.domain.policy.repository.PolicyRepository;
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

/**
 * 연계 추천의 연령 단위를 고정한다.
 *
 * <p>정책의 {@code targetAgeMin/Max} 는 <b>개월</b>이다 — 시드 데이터가 "부모급여(0세) 0~11",
 * "아동수당 0~95" 로 넣는다. 그런데 이 추천은 {@code Period.between(...).getYears()} 로
 * <b>연 나이</b>를 계산해 그 쿼리에 넘겼다. 세 살 아이(36개월)가 "3" 으로 들어가므로
 * <b>0~11개월 대상 정책(부모급여·첫만남이용권·산후조리비)이 추천된다.</b>
 *
 * <p>월령 계산이 코드 여섯 군데에 복제돼 있었는데, 그중 이 한 곳만 단위가 달랐다. 복제가
 * 위험한 이유가 이것이다 — 나머지 다섯 곳을 보고는 이 하나가 틀렸다는 걸 알 수 없다.
 *
 * <p>등록된 자녀가 없을 때도 같은 문제가 있었다. {@code .orElse(0)} 이 0개월로 바꿔 신생아
 * 정책을 추천했다. 모르는 값은 모르는 값으로 둬야 한다.
 */
@SpringBootTest(
        classes = CareCodeApplication.class,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration",
                "spring.cache.type=none",
                "spring.datasource.url=jdbc:h2:mem:carecode_health_reco;MODE=MySQL;DB_CLOSE_DELAY=-1",
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
@DisplayName("연계 추천 계약")
class HealthRecommendationContractTest {

    @MockBean RedisConnectionFactory redisConnectionFactory;
    @MockBean StringRedisTemplate stringRedisTemplate;
    @MockBean JavaMailSender javaMailSender;

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired JwtService jwtService;
    @Autowired UserRepository userRepository;
    @Autowired ChildRepository childRepository;
    @Autowired PolicyRepository policyRepository;

    private User parent;

    @BeforeEach
    void setUp() {
        policyRepository.deleteAll();
        parent = saveUser();

        // 시드 데이터와 같은 단위(개월)로 넣는다.
        savePolicy("부모급여(0세)", 0, 11);
        savePolicy("세 살 대상 지원금", 24, 47);
    }

    @Test
    @DisplayName("세 살 아이에게 0~11개월 대상 정책을 추천하지 않는다")
    void doesNotRecommendNewbornPoliciesToAThreeYearOld() throws Exception {
        saveChild(LocalDate.now().minusMonths(36));

        JsonNode body = json(mockMvc.perform(get("/health/recommendations")
                .header("Authorization", "Bearer " + token(parent))).andReturn());

        assertThat(body.path("childAge").asInt())
                .as("개월로 와야 한다. 3 이면 연 나이를 그대로 넘긴 것이다")
                .isEqualTo(36);

        var titles = toList(body);
        assertThat(titles).as("세 살 아이의 추천").contains("세 살 대상 지원금");
        assertThat(titles).as("0~11개월 대상은 들어가면 안 된다").doesNotContain("부모급여(0세)");
    }

    /**
     * 자녀가 없으면 월령을 알 수 없다. 0 으로 바꾸면 신생아 정책이 추천되고, 사용자는 그것을
     * "내 아이 기준 추천" 으로 읽는다.
     */
    @Test
    @DisplayName("등록된 자녀가 없으면 나이를 지어내지 않는다")
    void noChildMeansNoFabricatedAge() throws Exception {
        JsonNode body = json(mockMvc.perform(get("/health/recommendations")
                .header("Authorization", "Bearer " + token(parent))).andReturn());

        assertThat(body.path("childAge").isNull())
                .as("자녀가 없을 때 childAge: %s", body.path("childAge"))
                .isTrue();
        assertThat(toList(body)).as("나이를 모르면 연령 기반 추천을 내지 않는다").isEmpty();
    }

    @Test
    @DisplayName("생년월일이 없는 자녀만 있어도 나이를 지어내지 않는다")
    void missingBirthDateMeansNoFabricatedAge() throws Exception {
        saveChild(null);

        JsonNode body = json(mockMvc.perform(get("/health/recommendations")
                .header("Authorization", "Bearer " + token(parent))).andReturn());

        assertThat(body.path("childAge").isNull()).isTrue();
    }

    private java.util.List<String> toList(JsonNode body) {
        java.util.List<String> titles = new java.util.ArrayList<>();
        body.path("recommendedPolicies").forEach(n -> titles.add(n.asText()));
        return titles;
    }

    private void savePolicy(String title, int ageMinMonths, int ageMaxMonths) {
        policyRepository.save(Policy.builder()
                .policyCode("TEST-" + UUID.randomUUID().toString().substring(0, 8))
                .title(title)
                .description(title)
                .targetAgeMin(ageMinMonths)
                .targetAgeMax(ageMaxMonths)
                .isActive(true)
                .build());
    }

    private void saveChild(LocalDate birthDate) {
        childRepository.save(Child.builder()
                .user(parent)
                .name("아이")
                .birthDate(birthDate)
                .gender("FEMALE")
                .createdAt(LocalDateTime.now())
                .build());
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
