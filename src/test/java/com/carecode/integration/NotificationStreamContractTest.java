package com.carecode.integration;

import com.carecode.CareCodeApplication;
import com.carecode.domain.notification.entity.Notification;
import com.carecode.domain.notification.realtime.NotificationStreamService;
import com.carecode.domain.notification.repository.NotificationRepository;
import com.carecode.domain.user.entity.User;
import com.carecode.domain.user.entity.UserRole;
import com.carecode.domain.user.repository.UserRepository;
import com.carecode.domain.user.service.JwtService;
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
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * 알림 실시간 채널을 실제 필터 체인(JWT)과 실제 트랜잭션으로 확인한다.
 */
@SpringBootTest(
        classes = CareCodeApplication.class,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration",
                "spring.cache.type=none",
                "spring.datasource.url=jdbc:h2:mem:carecode_notification_stream;MODE=MySQL;DB_CLOSE_DELAY=-1",
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
@DisplayName("알림 실시간 채널 (SSE)")
class NotificationStreamContractTest {

    @MockBean RedisConnectionFactory redisConnectionFactory;
    @MockBean StringRedisTemplate stringRedisTemplate;
    @MockBean JavaMailSender javaMailSender;

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired UserRepository userRepository;
    @Autowired NotificationRepository notificationRepository;
    @Autowired NotificationStreamService streamService;
    @Autowired TransactionTemplate transactionTemplate;

    private User receiver;
    private User other;

    @BeforeEach
    void setUp() {
        receiver = saveUser();
        other = saveUser();
    }

    @Test
    @DisplayName("로그인 없이는 연결할 수 없다")
    void requiresLogin() throws Exception {
        assertThat(mockMvc.perform(get("/notifications/stream")).andReturn().getResponse().getStatus())
                .isEqualTo(401);
    }

    @Test
    @DisplayName("커밋된 알림은 받는 사람의 연결로만 간다")
    void deliversCommittedNotificationToOwnerOnly() throws Exception {
        MvcResult mine = connect(receiver);
        MvcResult others = connect(other);
        assertThat(body(mine)).contains("event:connected");

        Long id = transactionTemplate.execute(status -> notificationRepository.save(notification(receiver, "빈자리 알림")).getId());

        assertThat(body(mine))
                .contains("event:notification")
                .contains("id:" + id)
                .contains("\"title\":\"빈자리 알림\"")
                .contains("\"isRead\":false");
        assertThat(body(others)).doesNotContain("빈자리 알림");
    }

    @Test
    @DisplayName("롤백된 알림은 보내지 않는다")
    void rolledBackNotificationIsNotSent() throws Exception {
        MvcResult mine = connect(receiver);

        transactionTemplate.executeWithoutResult(status -> {
            notificationRepository.saveAndFlush(notification(receiver, "취소될 알림"));
            status.setRollbackOnly();
        });

        assertThat(body(mine)).doesNotContain("취소될 알림");
    }

    @Test
    @DisplayName("사용자당 연결 수에 상한이 있다")
    void connectionCapPerUser() throws Exception {
        int before = streamService.connectionCount();
        for (int i = 0; i < 8; i++) {
            connect(receiver);
        }
        assertThat(streamService.connectionCount() - before).isLessThanOrEqualTo(5);
    }

    private MvcResult connect(User user) throws Exception {
        String token = jwtService.generateAccessToken(user.getUserId(), user.getEmail(), user.getRole().name());
        MvcResult result = mockMvc.perform(get("/notifications/stream").header("Authorization", "Bearer " + token))
                .andReturn();
        assertThat(result.getRequest().isAsyncStarted()).as("SSE 는 비동기 응답이다").isTrue();
        assertThat(result.getResponse().getHeader("X-Accel-Buffering")).isEqualTo("no");
        return result;
    }

    private static String body(MvcResult result) throws Exception {
        return result.getResponse().getContentAsString(StandardCharsets.UTF_8).replace(": ", ":");
    }

    private static Notification notification(User user, String title) {
        return Notification.builder()
                .user(user)
                .notificationType(Notification.NotificationType.FACILITY)
                .title(title)
                .message("자리가 났어요")
                .build();
    }

    private User saveUser() {
        String id = UUID.randomUUID().toString().substring(0, 8);
        return userRepository.save(User.builder()
                .userId("user_" + id)
                .email(id + "@example.com")
                .password("{noop}unused")
                .name("사용자" + id)
                .role(UserRole.PARENT)
                .isActive(true)
                .emailVerified(true)
                .registrationCompleted(true)
                .createdAt(LocalDateTime.now())
                .build());
    }
}
