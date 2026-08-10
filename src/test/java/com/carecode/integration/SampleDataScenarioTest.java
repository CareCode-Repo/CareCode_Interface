package com.carecode.integration;

import com.carecode.CareCodeApplication;
import com.carecode.core.devtools.SampleFacilitySeeder;
import com.carecode.core.devtools.SamplePolicySeeder;
import com.carecode.core.security.CurrentUserFacade;
import com.carecode.domain.careFacility.dto.response.AdmissionForecastResponse;
import com.carecode.domain.careFacility.dto.response.FacilityPopularityResponse;
import com.carecode.domain.careFacility.entity.CareFacility;
import com.carecode.domain.careFacility.repository.CareFacilityRepository;
import com.carecode.domain.careFacility.service.AdmissionForecastService;
import com.carecode.domain.careFacility.service.FacilityPopularityService;
import com.carecode.domain.policy.dto.response.MissedBenefitSummaryResponse;
import com.carecode.domain.policy.dto.response.RegionalBenefitComparisonResponse;
import com.carecode.domain.policy.service.MissedBenefitService;
import com.carecode.domain.policy.service.RegionalBenefitComparisonService;
import com.carecode.domain.user.entity.Child;
import com.carecode.domain.user.entity.User;
import com.carecode.domain.user.entity.UserRole;
import com.carecode.domain.user.repository.ChildRepository;
import com.carecode.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/** 샘플 데이터를 넣고 신규 기능 3종이 실제로 값을 내는지 확인한다. */
@SpringBootTest(
        classes = CareCodeApplication.class,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration",
                "spring.cache.type=none",
                "spring.batch.job.enabled=false",
                "spring.datasource.url=jdbc:h2:mem:sample;MODE=MySQL;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.flyway.enabled=false",
                "app.search.fulltext-enabled=false",
                "jwt.secret=testJwtSecretKeyForSampleDataScenarioMustBe256BitsLong0123456789",
                "springdoc.api-docs.enabled=false",
                "springdoc.swagger-ui.enabled=false",
                "public.data.api.key=dummy",
                "KAKAO_CLIENT_ID=dummy-kakao-client",
                "KAKAO_CLIENT_SECRET=dummy-kakao-secret",
                "MAIL_USERNAME=dummy",
                "MAIL_PASSWORD=dummy"
        }
)
@DisplayName("샘플 데이터 기반 신규 기능 시나리오")
class SampleDataScenarioTest {

    @MockBean
    private RedisConnectionFactory redisConnectionFactory;
    @MockBean
    private StringRedisTemplate stringRedisTemplate;
    @MockBean
    private JavaMailSender javaMailSender;

    /** 인증 컨텍스트 없이 서비스 계층만 검증한다. */
    @MockBean
    private CurrentUserFacade currentUserFacade;

    @Autowired
    private SamplePolicySeeder policySeeder;
    @Autowired
    private SampleFacilitySeeder facilitySeeder;
    @Autowired
    private RegionalBenefitComparisonService regionalBenefitComparisonService;
    @Autowired
    private MissedBenefitService missedBenefitService;
    @Autowired
    private AdmissionForecastService admissionForecastService;
    @Autowired
    private FacilityPopularityService facilityPopularityService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ChildRepository childRepository;
    @Autowired
    private CareFacilityRepository facilityRepository;

    @BeforeEach
    void setUp() {
        policySeeder.seed();
        facilitySeeder.seed();

        User user = userRepository.findByEmail("sample@carecode.test").orElseGet(() ->
                userRepository.save(User.builder()
                        .userId("sample-user")
                        .email("sample@carecode.test")
                        .name("샘플부모")
                        .address("경기도 성남시 분당구")
                        .role(UserRole.PARENT)
                        .isActive(true)
                        .emailVerified(true)
                        .registrationCompleted(true)
                        .build()));

        if (childRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).isEmpty()) {
            childRepository.save(Child.builder()
                    .user(user)
                    .name("샘플아이")
                    .birthDate(LocalDate.now().minusMonths(30))
                    .build());
        }
        when(currentUserFacade.requireCurrentUser()).thenReturn(user);
    }

    @Test
    @DisplayName("거주지별 지원금 비교가 지역 간 차액을 계산한다")
    void comparesRegionalBenefits() {
        RegionalBenefitComparisonResponse result =
                regionalBenefitComparisonService.compare(null, 5, 10);

        assertThat(result.getRankings()).isNotEmpty();
        assertThat(result.getBaseRegion()).isEqualTo("성남시");
        assertThat(result.getDataQuality()).isEqualTo("ESTIMATED");

        // 인구감소지역 지원금이 수도권보다 커야 비교가 의미를 갖는다
        var top = result.getRankings().get(0);
        assertThat(top.getDifferenceFromBase()).isPositive();
        assertThat(top.getTopContributors()).isNotEmpty();
    }

    @Test
    @DisplayName("놓친 지원금이 소급 가능/만료로 분류된다")
    void findsMissedBenefits() {
        // 30개월 아이 → 0~11개월, 0~23개월 대상 정책 구간을 이미 지났다
        MissedBenefitSummaryResponse result = missedBenefitService.findMissedBenefits();

        assertThat(result.getClaimableCount() + result.getExpiredCount()).isPositive();
    }

    @Test
    @DisplayName("늘 만원인 시설은 입소 확률이 낮고 인기 시설로 분류된다")
    void alwaysFullFacilityIsInDemand() {
        CareFacility facility = findSample("ALWAYS_FULL");

        FacilityPopularityResponse popularity = facilityPopularityService.analyze(facility.getId());
        assertThat(popularity.isAvailable()).isTrue();
        assertThat(popularity.getDemandLevel()).isEqualTo("IN_DEMAND");

        AdmissionForecastResponse forecast =
                admissionForecastService.forecast(facility.getId(), 30, 1);
        assertThat(forecast.isAvailable()).isTrue();
        assertThat(forecast.getProbability()).isNotNull();
    }

    @Test
    @DisplayName("정원 미달 시설은 여유 시설로 분류되고 입소 확률이 높다")
    void undersubscribedFacilityIsEasyToEnter() {
        CareFacility facility = findSample("UNDERSUBSCRIBED");

        assertThat(facilityPopularityService.analyze(facility.getId()).getDemandLevel())
                .isEqualTo("UNDERSUBSCRIBED");
        assertThat(admissionForecastService.forecast(facility.getId(), 30, 1).getProbability())
                .isGreaterThan(50);
    }

    @Test
    @DisplayName("충원율이 내려가는 시설은 하락 추세로 잡힌다")
    void decliningFacilityShowsFallingTrend() {
        assertThat(facilityPopularityService.analyze(findSample("DECLINING").getId()).getTrend())
                .isEqualTo("FALLING");
    }

    @Test
    @DisplayName("급락 시설은 변동 시점을 기록한다")
    void sharpDropIsRecorded() {
        assertThat(facilityPopularityService.analyze(findSample("SHARP_DROP").getId())
                .getSharpDropDates()).isNotEmpty();
    }

    @Test
    @DisplayName("샘플 데이터를 두 번 넣어도 중복되지 않는다")
    void seedingIsIdempotent() {
        long before = facilityRepository.count();

        policySeeder.seed();
        facilitySeeder.seed();

        assertThat(facilityRepository.count()).isEqualTo(before);
    }

    private CareFacility findSample(String pattern) {
        List<CareFacility> all = facilityRepository.findAll();
        return all.stream()
                .filter(f -> f.getFacilityCode() != null && f.getFacilityCode().endsWith(pattern))
                .findFirst()
                .orElseThrow(() -> new AssertionError("샘플 시설을 찾을 수 없습니다: " + pattern));
    }
}
