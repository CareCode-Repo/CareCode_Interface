package com.carecode.integration;

import com.carecode.CareCodeApplication;
import com.carecode.domain.careFacility.entity.CareFacility;
import com.carecode.domain.careFacility.entity.FacilityType;
import com.carecode.domain.careFacility.repository.CareFacilityRepository;
import com.carecode.domain.health.entity.Hospital;
import com.carecode.domain.health.repository.HospitalRepository;
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
