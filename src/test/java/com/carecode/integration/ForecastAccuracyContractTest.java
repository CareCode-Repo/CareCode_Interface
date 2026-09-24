package com.carecode.integration;

import com.carecode.CareCodeApplication;
import com.carecode.domain.careFacility.entity.CareFacility;
import com.carecode.domain.careFacility.entity.FacilityCapacitySnapshot;
import com.carecode.domain.careFacility.entity.FacilityType;
import com.carecode.domain.careFacility.repository.CareFacilityRepository;
import com.carecode.domain.careFacility.repository.FacilityCapacitySnapshotRepository;
import com.carecode.domain.user.entity.User;
import com.carecode.domain.user.entity.UserRole;
import com.carecode.domain.user.repository.UserRepository;
import com.carecode.domain.user.service.JwtService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * 예측 정확도를 측정하고 공개하는 경로 전체.
 *
 * <p>확률을 보여주면서 그 확률이 맞는지 확인하지 않던 상태를 고친 기능이라, "측정 전에는 비어 있고
 * 측정 후에는 표본과 함께 나온다" 는 것이 계약이다.
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
                "spring.datasource.url=jdbc:h2:mem:carecode_forecast_accuracy;MODE=MySQL;DB_CLOSE_DELAY=-1",
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
@DisplayName("입소 예측 정확도")
class ForecastAccuracyContractTest {

    @MockBean RedisConnectionFactory redisConnectionFactory;
    @MockBean StringRedisTemplate stringRedisTemplate;
    @MockBean JavaMailSender javaMailSender;

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired JwtService jwtService;
    @Autowired UserRepository userRepository;
    @Autowired CareFacilityRepository careFacilityRepository;
    @Autowired FacilityCapacitySnapshotRepository snapshotRepository;

    @Test
    @DisplayName("측정 전에는 공개 정확도가 비어 있고, 측정하면 표본과 구간표가 나온다")
    void measureThenPublish() throws Exception {
        assertThat(publicAccuracy()).isEmpty();

        List<Long> facilityIds = List.of(seedFacility(true), seedFacility(false), seedFacility(true));

        // 수동 측정은 관리자만 할 수 있다.
        assertThat(mockMvc.perform(post("/api/admin/sync/forecast-accuracy/measure")
                        .header("Authorization", "Bearer " + token(saveUser(UserRole.PARENT))))
                .andReturn().getResponse().getStatus()).isEqualTo(403);

        MvcResult measured = mockMvc.perform(post("/api/admin/sync/forecast-accuracy/measure")
                        .header("Authorization", "Bearer " + token(saveUser(UserRole.ADMIN))))
                .andReturn();
        String measuredBody = measured.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(measured.getResponse().getStatus()).as(measuredBody).isEqualTo(200);
        assertThat(objectMapper.readTree(measuredBody).path("measured").asInt()).isPositive();

        JsonNode accuracy = publicAccuracy();
        assertThat(accuracy).isNotEmpty();
        JsonNode oneMonth = findHorizon(accuracy, 1);
        assertThat(oneMonth.path("samples").asInt()).isGreaterThan(30);
        assertThat(oneMonth.path("facilities").asInt()).isEqualTo(facilityIds.size());
        assertThat(oneMonth.path("calibration").isArray()).isTrue();
        assertThat(oneMonth.path("brierScore").isNumber()).isTrue();
        assertThat(oneMonth.has("betterThanBaseline")).isTrue();

        // 예측 응답에도 같은 확률대의 실제 적중률이 붙는다.
        JsonNode forecast = json(mockMvc.perform(get("/facilities/{id}/admission-forecast", facilityIds.get(0))
                .param("horizonMonths", "1")).andReturn());
        assertThat(forecast.path("available").asBoolean()).isTrue();
        assertThat(forecast.path("accuracy").path("samples").asInt()).isGreaterThan(30);
        assertThat(forecast.path("accuracy").path("horizonMonths").asInt()).isEqualTo(1);
    }

    private JsonNode publicAccuracy() throws Exception {
        MvcResult result = mockMvc.perform(get("/facilities/forecast-accuracy")).andReturn();
        String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(result.getResponse().getStatus()).as("비로그인도 볼 수 있어야 한다: %s", body).isEqualTo(200);
        return objectMapper.readTree(body);
    }

    private static JsonNode findHorizon(JsonNode accuracy, int horizon) {
        for (JsonNode node : accuracy) {
            if (node.path("horizonMonths").asInt() == horizon) {
                return node;
            }
        }
        throw new AssertionError(horizon + "개월 측정 결과가 없습니다: " + accuracy);
    }

    /** 20개월치 주간 관측. 자리가 자주 나는 시설과 거의 나지 않는 시설을 섞어 구간이 퍼지게 한다. */
    private Long seedFacility(boolean opensOften) {
        Long facilityId = careFacilityRepository.save(CareFacility.builder()
                .facilityCode("F-" + UUID.randomUUID())
                .name("행복 어린이집")
                .facilityType(FacilityType.DAYCARE)
                .isActive(true)
                .capacity(100)
                .build()).getId();

        LocalDate start = LocalDate.now().minusMonths(20);
        List<FacilityCapacitySnapshot> snapshots = new ArrayList<>();
        for (int week = 0; week < 85; week++) {
            int spots = opensOften ? (week % 2 == 0 ? 3 : 0) : (week % 20 == 0 ? 1 : 0);
            snapshots.add(FacilityCapacitySnapshot.builder()
                    .facilityId(facilityId)
                    .observedDate(start.plusWeeks(week))
                    .capacity(100)
                    .currentEnrollment(100 - spots)
                    .availableSpots(spots)
                    .createdAt(LocalDateTime.now())
                    .build());
        }
        snapshotRepository.saveAll(snapshots);
        return facilityId;
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private String token(User user) {
        return jwtService.generateAccessToken(user.getUserId(), user.getEmail(), user.getRole().name());
    }

    private User saveUser(UserRole role) {
        String id = UUID.randomUUID().toString().substring(0, 8);
        return userRepository.save(User.builder()
                .userId("user_" + id)
                .email(id + "@example.com")
                .password("{noop}unused")
                .name("사용자" + id)
                .role(role)
                .isActive(true)
                .emailVerified(true)
                .registrationCompleted(true)
                .createdAt(LocalDateTime.now())
                .build());
    }
}
