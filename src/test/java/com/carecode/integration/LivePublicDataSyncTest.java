package com.carecode.integration;

import com.carecode.CareCodeApplication;
import com.carecode.core.client.sync.GovernmentBenefitSyncService;
import com.carecode.core.client.sync.NationwideChildcareFacilitySyncService;
import com.carecode.core.client.sync.KindergartenSyncService;
import com.carecode.core.client.sync.PediatricHospitalSyncService;
import com.carecode.core.client.sync.SyncResult;
import com.carecode.domain.careFacility.entity.CareFacility;
import com.carecode.domain.careFacility.repository.CareFacilityRepository;
import com.carecode.domain.careFacility.repository.FacilityCapacitySnapshotRepository;
import com.carecode.domain.health.repository.HospitalRepository;
import com.carecode.domain.policy.repository.PolicyRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 실제 공공데이터 API 를 호출해 적재까지 확인한다.
 * 외부 의존이 있어 일반 빌드에서는 제외되고, 연동 점검이 필요할 때만 수동으로 돌린다.
 *
 * 실행: ./gradlew test --tests '*LivePublicDataSyncTest' -DincludeTags=live \
 *        -DKINDERGARTEN_INFO_KEY=... -DDATA_GO_KR_SERVICE_KEY=...
 */
@Tag("live")
@SpringBootTest(
        classes = CareCodeApplication.class,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration",
                "spring.cache.type=none",
                "spring.batch.job.enabled=false",
                "spring.datasource.url=jdbc:h2:mem:live;MODE=MySQL;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.flyway.enabled=false",
                "app.search.fulltext-enabled=false",
                // 전국을 다 돌면 오래 걸린다. 연동 확인에는 몇 페이지면 충분하다.
                "public.data.sync.max-pages=2",
                "jwt.secret=liveSyncTestSecretKeyMustBeAtLeast256BitsLong0123456789abc",
                "springdoc.api-docs.enabled=false",
                "springdoc.swagger-ui.enabled=false",
                "public.data.api.key=dummy",
                "KAKAO_CLIENT_ID=d", "KAKAO_CLIENT_SECRET=d",
                "MAIL_USERNAME=d", "MAIL_PASSWORD=d"
        }
)
@DisplayName("공공데이터 실연동 점검")
class LivePublicDataSyncTest {

    @MockBean private RedisConnectionFactory redisConnectionFactory;
    @MockBean private StringRedisTemplate stringRedisTemplate;
    @MockBean private JavaMailSender javaMailSender;

    @Autowired private KindergartenSyncService kindergartenSync;
    @Autowired private NationwideChildcareFacilitySyncService childcareSync;
    @Autowired private GovernmentBenefitSyncService benefitSync;
    @Autowired private PediatricHospitalSyncService hospitalSync;
    @Autowired private CareFacilityRepository facilityRepository;
    @Autowired private FacilityCapacitySnapshotRepository snapshotRepository;
    @Autowired private PolicyRepository policyRepository;
    @Autowired private HospitalRepository hospitalRepository;

    @Test
    @DisplayName("유치원: 시설과 정원 스냅샷이 함께 적재된다")
    void syncsKindergartens() {
        SyncResult result = kindergartenSync.sync();
        System.out.println("@@ 유치원 " + result);

        assertThat(result.getCreated()).isPositive();
        List<CareFacility> saved = facilityRepository.findAll();
        assertThat(saved).isNotEmpty();

        CareFacility sample = saved.get(0);
        assertThat(sample.getName()).isNotBlank();
        assertThat(sample.getCapacity()).isNotNull();
        assertThat(sample.getCurrentEnrollment()).isNotNull();
        // 위경도가 없으면 반경 검색이 무의미해진다
        assertThat(sample.getLatitude()).isNotNull();
        // 스냅샷이 쌓여야 입소 예측이 가능해진다
        assertThat(snapshotRepository.count()).isPositive();

        System.out.println("@@ 예시: " + sample.getName() + " / 정원 " + sample.getCapacity()
                + " 현원 " + sample.getCurrentEnrollment() + " / " + sample.getAddress());
    }

    @Test
    @DisplayName("정책: 지자체 정책의 지역명이 기관명이 아니라 지역으로 들어간다")
    void syncsBenefits() {
        SyncResult result = benefitSync.sync();
        System.out.println("@@ 정책 " + result);

        assertThat(result.getTotalProcessed()).isPositive();
        var policies = policyRepository.findByIsActiveTrue();
        assertThat(policies).isNotEmpty();

        policies.stream().limit(5).forEach(p ->
                System.out.println("@@ 정책: [" + p.getTargetRegion() + "] " + p.getTitle()));
        // "교육부" 같은 기관명이 지역으로 들어가면 거주지 비교가 깨진다
        assertThat(policies).allSatisfy(p -> assertThat(p.getTargetRegion()).isNotBlank());
    }

    @Test
    @DisplayName("병원: 진료과목과 종별이 분리 저장된다")
    void syncsHospitals() {
        SyncResult result = hospitalSync.sync();
        System.out.println("@@ 병원 " + result);

        assertThat(result.getCreated()).isPositive();
        var hospitals = hospitalRepository.findAll();
        assertThat(hospitals).isNotEmpty();

        hospitals.stream().limit(5).forEach(h -> System.out.println(
                "@@ 병원: " + h.getName() + " / " + h.getType() + " / " + h.getGrade()
                        + " / " + h.getLatitude() + "," + h.getLongitude()));

        assertThat(hospitals).allSatisfy(h -> {
            assertThat(h.getType()).isEqualTo("소아청소년과");
            // 위도는 33~39, 경도는 124~132 범위다. 뒤집히면 여기서 잡힌다.
            if (h.getLatitude() != null) {
                assertThat(h.getLatitude()).isBetween(33.0, 39.0);
                assertThat(h.getLongitude()).isBetween(124.0, 132.0);
            }
        });
    }

    @Test
    @DisplayName("어린이집: 시설코드·정원이 적재된다")
    void syncsChildcareFacilities() {
        SyncResult result = childcareSync.sync();
        System.out.println("@@ 어린이집 " + result);

        assertThat(result.getCreated()).isPositive();
        CareFacility sample = facilityRepository.findAll().get(0);
        System.out.println("@@ 예시: " + sample.getName() + " / 정원 " + sample.getCapacity()
                + " / " + sample.getPhone() + " / " + sample.getAddress());

        assertThat(sample.getName()).isNotBlank();
        assertThat(sample.getCapacity()).isNotNull();
    }
}
