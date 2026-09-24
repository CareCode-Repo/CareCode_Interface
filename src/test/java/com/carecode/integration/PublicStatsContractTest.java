package com.carecode.integration;

import com.carecode.CareCodeApplication;
import com.carecode.domain.careFacility.entity.CareFacility;
import com.carecode.domain.careFacility.entity.FacilityType;
import com.carecode.domain.careFacility.repository.CareFacilityRepository;
import com.carecode.domain.health.entity.Hospital;
import com.carecode.core.ops.sync.SyncJob;
import com.carecode.core.ops.sync.SyncRunTracker;
import com.carecode.domain.health.repository.HospitalRepository;
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
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * 소개 사이트가 로그인 없이 가져가는 수집 현황. 필드가 있으면 값도 있어야 한다.
 * 시설 통계는 유형별 집계를 조회해 놓고 null 을, 활성 시설 수는 0 을 내보내고 있었다.
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
                "spring.datasource.url=jdbc:h2:mem:carecode_public_stats;MODE=MySQL;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.flyway.enabled=false",
                // 신선도 캐시를 끄고 방금 기록한 이력이 바로 보이게 한다.
                "app.sync.freshness.cache-ttl-seconds=0",
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
@DisplayName("공개 통계")
class PublicStatsContractTest {

    @MockBean RedisConnectionFactory redisConnectionFactory;
    @MockBean StringRedisTemplate stringRedisTemplate;
    @MockBean JavaMailSender javaMailSender;

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired CareFacilityRepository careFacilityRepository;
    @Autowired HospitalRepository hospitalRepository;
    @Autowired SyncRunTracker syncRunTracker;
    @Autowired UserRepository userRepository;
    @Autowired JwtService jwtService;

    @Test
    @DisplayName("시설 통계는 유형별 분포와 활성 시설 수를 실제 값으로 준다")
    void facilityStatisticsAreFilled() throws Exception {
        saveFacility(FacilityType.DAYCARE, true);
        saveFacility(FacilityType.DAYCARE, true);
        saveFacility(FacilityType.KINDERGARTEN, true);
        saveFacility(FacilityType.KINDERGARTEN, false);

        JsonNode stats = getJson("/facilities/statistics");

        assertThat(stats.path("totalFacilities").asLong()).isGreaterThanOrEqualTo(4);
        assertThat(stats.path("activeFacilities").asLong()).isGreaterThanOrEqualTo(3)
                .isLessThan(stats.path("totalFacilities").asLong());
        assertThat(stats.path("typeDistribution").path("DAYCARE").asLong()).isGreaterThanOrEqualTo(2);
        assertThat(stats.path("typeStats").isArray()).isTrue();
        assertThat(stats.path("typeStats")).isNotEmpty();
    }

    @Test
    @DisplayName("병원 통계는 로그인 없이 전체 수와 진료과목별 수를 준다")
    void hospitalStatistics() throws Exception {
        saveHospital("소아청소년과");
        saveHospital("소아청소년과");
        saveHospital(null);

        JsonNode stats = getJson("/health/hospitals/statistics");

        assertThat(stats.path("totalHospitals").asLong()).isGreaterThanOrEqualTo(3);
        assertThat(stats.path("byType").path("소아청소년과").asLong()).isGreaterThanOrEqualTo(2);
        assertThat(stats.path("byType").path("기타").asLong()).isGreaterThanOrEqualTo(1);
    }

    /**
     * 공개 통계는 "언제 기준 수치인가" 를 함께 줘야 한다. 소개 사이트가 이 값을 그대로 표시한다.
     * 동기화가 한 번도 돌지 않았으면 현재 시각으로 속이지 않고 null 이다.
     */
    @Test
    @DisplayName("공개 통계는 데이터 기준 시각을 함께 준다")
    void statisticsExposeDataUpdatedAt() throws Exception {
        assertThat(getJson("/facilities/statistics").path("dataUpdatedAt").isNull()).isTrue();

        LocalDateTime startedAt = LocalDateTime.now().minusMinutes(1);
        syncRunTracker.recordSuccess(SyncJob.CHILDCARE_FACILITIES, startedAt, 120, 0, "테스트");
        syncRunTracker.recordSuccess(SyncJob.KINDERGARTENS, startedAt, 80, 0, "테스트");
        syncRunTracker.recordSuccess(SyncJob.PEDIATRIC_HOSPITALS, startedAt, 40, 0, "테스트");

        assertThat(getJson("/facilities/statistics").path("dataUpdatedAt").asText()).isNotEmpty();
        assertThat(getJson("/health/hospitals/statistics").path("dataUpdatedAt").asText()).isNotEmpty();
    }

    @Test
    @DisplayName("작업 상태 조회는 관리자만 볼 수 있다")
    void syncStatusIsAdminOnly() throws Exception {
        assertThat(mockMvc.perform(get("/api/admin/sync/status")).andReturn().getResponse().getStatus())
                .isEqualTo(401);
        assertThat(status(saveUser(UserRole.PARENT), "/api/admin/sync/status")).isEqualTo(403);

        MvcResult result = mockMvc.perform(get("/api/admin/sync/status")
                        .header("Authorization", "Bearer " + token(saveUser(UserRole.ADMIN))))
                .andReturn();
        String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(result.getResponse().getStatus()).as(body).isEqualTo(200);

        JsonNode json = objectMapper.readTree(body);
        assertThat(json.path("jobs")).hasSize(SyncJob.values().length);
        assertThat(json.path("jobs").findValuesAsText("job")).contains("childcare-facilities");
        assertThat(json.path("staleCount").isNumber()).isTrue();
    }

    private int status(User user, String path) throws Exception {
        return mockMvc.perform(get(path).header("Authorization", "Bearer " + token(user)))
                .andReturn().getResponse().getStatus();
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

    private JsonNode getJson(String path) throws Exception {
        MvcResult result = mockMvc.perform(get(path)).andReturn();
        String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(result.getResponse().getStatus()).as(body).isEqualTo(200);
        return objectMapper.readTree(body);
    }

    private void saveFacility(FacilityType type, boolean active) {
        careFacilityRepository.save(CareFacility.builder()
                .facilityCode("F-" + UUID.randomUUID())
                .name("시설")
                .facilityType(type)
                .isActive(active)
                .build());
    }

    private void saveHospital(String type) {
        hospitalRepository.save(Hospital.builder()
                .name("병원-" + UUID.randomUUID())
                .type(type)
                .build());
    }
}
