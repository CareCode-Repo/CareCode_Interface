package com.carecode.integration;

import com.carecode.CareCodeApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 마이그레이션만으로 만든 스키마가 엔티티와 일치하는지 검증한다.
 *
 * <p>다른 통합 테스트는 전부 {@code ddl-auto=create-drop} 이라 Hibernate 가 엔티티에서
 * 스키마를 만들어 낸다. 그래서 Flyway 가 아무리 어긋나도 통과한다. 실제로 정책 북마크는
 * 테이블 없이 API 와 리포지토리까지 있었고, 조회수는 컬럼이 없어 저장된 적이 없었는데
 * 기존 테스트는 전부 초록불이었다.
 *
 * <p>여기서는 운영과 같은 방식(Flyway 전체 적용 + {@code validate})으로 띄운다.
 * 엔티티에 필드를 추가하고 마이그레이션을 안 쓰면 이 테스트가 기동 단계에서 깨진다.
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
                "jwt.secret=testJwtSecretKeyForIntegrationTestsMustBe256BitsLong012345678901234567890",
                // 운영과 동일하게: 스키마는 Flyway 가 만들고 Hibernate 는 검증만 한다.
                "spring.flyway.enabled=true",
                "spring.jpa.hibernate.ddl-auto=validate",
                "springdoc.api-docs.enabled=false",
                "springdoc.swagger-ui.enabled=false",
                "public.data.api.key=dummy",
                "GOOGLE_CLIENT_ID=dummy-google-client",
                "GOOGLE_CLIENT_SECRET=dummy-google-secret",
                "KAKAO_CLIENT_ID=dummy-kakao-client",
                "KAKAO_CLIENT_SECRET=dummy-kakao-secret",
                "MAIL_USERNAME=dummy",
                "MAIL_PASSWORD=dummy"
        }
)
@Testcontainers(disabledWithoutDocker = true)
@DisplayName("Flyway 스키마와 엔티티 정합성")
class FlywaySchemaValidationTest {

    /**
     * Linux MariaDB 는 lower_case_table_names=0 이라 테이블 이름의 대소문자를 구분한다.
     * 운영과 같은 조건이어야 대문자 마이그레이션 / 소문자 매핑 불일치가 여기서 잡힌다.
     */
    @Container
    static final MariaDBContainer<?> MARIA_DB = new MariaDBContainer<>("mariadb:10.11")
            .withDatabaseName("carecode_schema")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MARIA_DB::getJdbcUrl);
        registry.add("spring.datasource.username", MARIA_DB::getUsername);
        registry.add("spring.datasource.password", MARIA_DB::getPassword);
    }

    @MockBean
    RedisConnectionFactory redisConnectionFactory;

    @MockBean
    StringRedisTemplate stringRedisTemplate;

    @MockBean
    JavaMailSender javaMailSender;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("마이그레이션만으로 만든 스키마로 컨텍스트가 뜬다")
    void contextLoadsOnMigratedSchema() {
        // ddl-auto=validate 라 엔티티와 어긋나면 여기 오기 전에 기동이 실패한다.
        Integer applied = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = 1", Integer.class);

        assertThat(applied)
                .as("적용된 마이그레이션")
                .isNotNull()
                .isGreaterThanOrEqualTo(15);
    }

    @Test
    @DisplayName("실패한 마이그레이션이 남아 있지 않다")
    void noFailedMigrations() {
        Integer failed = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = 0", Integer.class);

        assertThat(failed).as("실패한 마이그레이션").isZero();
    }

    @Test
    @DisplayName("한 번도 동작한 적 없던 테이블들이 실제로 만들어진다")
    void previouslyMissingTablesExist() {
        // V15 로 뒤늦게 채운 것들. 회귀하면 즉시 알아야 한다.
        List<String> mustExist = List.of("TBL_POLICY_BOOKMARKS", "TBL_NOTIFICATION_CHANNEL");

        for (String table : mustExist) {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.tables "
                            + "WHERE table_schema = DATABASE() AND table_name = ?",
                    Integer.class, table);

            assertThat(count).as("%s 테이블", table).isEqualTo(1);
        }
    }
}
