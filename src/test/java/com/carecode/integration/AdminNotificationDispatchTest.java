package com.carecode.integration;

import com.carecode.CareCodeApplication;
import com.carecode.domain.notification.entity.Notification;
import com.carecode.domain.notification.sender.NotificationDispatcher;
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
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * 어드민이 "알림 발송" 을 누르면 실제로 발송돼야 한다.
 *
 * <p>알림을 만드는 경로가 넷이었고, 그중 어드민 경로만 발송을 하지 않았다.
 *
 * <ul>
 *   <li>{@code NotificationCreationService.createAndSend} (예약·접종 리마인더) — 저장 + 발송</li>
 *   <li>{@code FacilityVacancyNotifier}, {@code PolicyDeadlineNotifier} — 저장 + {@code dispatchAsync}</li>
 *   <li>{@code NotificationService.createNotification} (사용자 API) — 저장 + {@code dispatchAsync}</li>
 *   <li><b>{@code POST /api/admin/notifications}</b> — <b>저장만.</b> 리포지토리에 직접 쓰고
 *       끝낸다</li>
 * </ul>
 *
 * <p>그래서 관리자가 보낸 알림은 앱 알림함에만 쌓이고 푸시·메일이 나가지 않는다. 엔드포인트
 * 설명은 "알림 발송" 이고 응답도 201 이라, 보낸 사람은 나갔다고 믿는다.
 */
@SpringBootTest(
        classes = CareCodeApplication.class,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration",
                "spring.cache.type=none",
                "spring.datasource.url=jdbc:h2:mem:carecode_admin_notify;MODE=MySQL;DB_CLOSE_DELAY=-1",
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
@DisplayName("어드민 알림 발송")
class AdminNotificationDispatchTest {

    @MockBean RedisConnectionFactory redisConnectionFactory;
    @MockBean StringRedisTemplate stringRedisTemplate;
    @MockBean JavaMailSender javaMailSender;

    /** 실제 SMTP·FCM 왕복 없이 "발송을 시도했는지" 만 본다. */
    @MockBean NotificationDispatcher dispatcher;

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired UserRepository userRepository;

    private User admin;
    private User recipient;

    @BeforeEach
    void setUp() {
        admin = saveUser(UserRole.ADMIN);
        recipient = saveUser(UserRole.PARENT);
    }

    @Test
    @DisplayName("관리자가 보낸 알림도 사용자 채널로 발송된다")
    void adminNotificationIsDispatched() throws Exception {
        String body = """
                {"userId": %d, "notificationType": "SYSTEM", "title": "점검 안내", "message": "오늘 02시에 점검합니다."}
                """.formatted(recipient.getId());

        int status = mockMvc.perform(post("/api/admin/notifications")
                        .header("Authorization", "Bearer " + token(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn().getResponse().getStatus();

        assertThat(status).as("생성 응답").isEqualTo(201);

        // 저장만 하고 끝내면 앱 알림함에만 쌓이고 푸시·메일이 나가지 않는다.
        verify(dispatcher, atLeastOnce()).dispatchAsync(any(Notification.class));
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
