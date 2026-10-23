package com.carecode.integration;

import com.carecode.CareCodeApplication;
import com.carecode.domain.user.entity.User;
import com.carecode.domain.user.entity.UserRole;
import com.carecode.domain.user.repository.UserRepository;
import com.carecode.domain.user.service.JwtService;
import org.junit.jupiter.api.BeforeAll;
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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * 건강기록 첨부는 정적 경로로 내려가지 않아야 한다.
 *
 * <p>업로드 루트는 {@code /files/**} 로 정적 서빙된다. 보안 설정은 그중
 * {@code /files/profile-images/**} 만 공개로 두고 나머지는 인증을 요구하며, 주석도 그렇게 적고
 * 있다 — "건강기록 첨부는 민감정보라 주소만 알면 열리는 상태로 두면 안 되고, 인증을 거치는
 * 별도 다운로드 경로가 필요하다".
 *
 * <p>그런데 <b>인증만으로는 부족하다.</b> 인증을 거치는 다운로드 경로
 * ({@code GET /health/records/{recordId}/attachments/{id}/download})는 소유권까지 확인하지만,
 * 정적 경로는 <b>로그인한 아무나</b> 통과시킨다. 주소를 알게 된 다른 회원이 남의 진단서를
 * 그대로 받아갈 수 있다. 파일명이 UUID 라 추측은 어렵지만, 추측이 유일한 방어라면 방어가 아니다.
 *
 * <p>그래서 정적 서빙을 프로필 이미지로만 좁힌다. 첨부는 소유권을 확인하는 경로로만 받는다.
 */
@SpringBootTest(
        classes = CareCodeApplication.class,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration",
                "spring.cache.type=none",
                "spring.datasource.url=jdbc:h2:mem:carecode_static_files;MODE=MySQL;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.flyway.enabled=false",
                "jwt.secret=testJwtSecretKeyForAccessControlTestMustBe256BitsLong0123456789",
                "springdoc.api-docs.enabled=false",
                "springdoc.swagger-ui.enabled=false",
                "app.storage.local.root=" + AttachmentStaticServingTest.STORAGE_ROOT,
                "public.data.api.key=dummy",
                "KAKAO_CLIENT_ID=dummy-kakao-client",
                "KAKAO_CLIENT_SECRET=dummy-kakao-secret",
                "MAIL_USERNAME=dummy",
                "MAIL_PASSWORD=dummy"
        }
)
@AutoConfigureMockMvc
@DisplayName("첨부 정적 서빙 차단")
class AttachmentStaticServingTest {

    /** 컨텍스트 프로퍼티로 넘겨야 해서 상수다. 빌드 디렉터리 안이라 정리 대상에 들어간다. */
    static final String STORAGE_ROOT = "build/test-uploads/static-serving";

    private static final String ATTACHMENT_PATH = "health-records/2026/10/23/secret-diagnosis.png";
    private static final String PROFILE_PATH = "profile-images/avatar.png";

    @MockBean RedisConnectionFactory redisConnectionFactory;
    @MockBean StringRedisTemplate stringRedisTemplate;
    @MockBean JavaMailSender javaMailSender;

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired UserRepository userRepository;

    private User stranger;

    /** 서빙 대상 파일을 실제로 만들어 둔다. 없으면 차단과 404 를 구분할 수 없다. */
    @BeforeAll
    static void createFiles() throws IOException {
        for (String relative : new String[] {ATTACHMENT_PATH, PROFILE_PATH}) {
            Path file = Path.of(STORAGE_ROOT).resolve(relative);
            Files.createDirectories(file.getParent());
            Files.writeString(file, "binary-ish", StandardCharsets.UTF_8);
        }
    }

    @BeforeEach
    void setUp() {
        stranger = saveUser();
    }

    @Test
    @DisplayName("로그인한 다른 회원도 정적 경로로 첨부를 받을 수 없다")
    void authenticatedStrangerCannotFetchAttachment() throws Exception {
        int status = mockMvc.perform(get("/files/{path}", ATTACHMENT_PATH)
                        .header("Authorization", "Bearer " + token(stranger)))
                .andReturn().getResponse().getStatus();

        assertThat(status)
                .as("로그인만 하면 남의 진단서를 받을 수 있으면 안 된다")
                .isNotEqualTo(200);
    }

    @Test
    @DisplayName("비로그인도 당연히 받을 수 없다")
    void anonymousCannotFetchAttachment() throws Exception {
        assertThat(mockMvc.perform(get("/files/{path}", ATTACHMENT_PATH))
                .andReturn().getResponse().getStatus())
                .isNotEqualTo(200);
    }

    /**
     * 차단이 과하면 프로필 이미지가 깨진다. {@code <img src>} 는 인증 헤더를 붙일 수 없어
     * 이 경로는 공개로 유지해야 한다.
     */
    @Test
    @DisplayName("프로필 이미지는 비로그인에도 계속 열린다")
    void profileImageStaysPublic() throws Exception {
        assertThat(mockMvc.perform(get("/files/{path}", PROFILE_PATH))
                .andReturn().getResponse().getStatus())
                .as("프로필 이미지는 화면이 직접 불러간다")
                .isEqualTo(200);
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
                .name("회원" + id)
                .role(UserRole.PARENT)
                .isActive(true)
                .emailVerified(true)
                .registrationCompleted(true)
                .createdAt(LocalDateTime.now())
                .build());
    }
}
