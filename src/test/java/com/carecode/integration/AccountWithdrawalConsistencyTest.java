package com.carecode.integration;

import com.carecode.CareCodeApplication;
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

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;

/**
 * 탈퇴 처리의 결과는 누가 눌렀는지와 무관해야 한다.
 *
 * <p>같은 "탈퇴 처리" 에 두 구현이 있었다.
 *
 * <ul>
 *   <li>{@code DELETE /users/account} (본인) — {@code PrivacyService.deleteMyAccount} 가
 *       이름·이메일·전화·주소·프로필·소셜 ID·좌표를 <b>지우거나 익명화</b>한 뒤 비활성화한다.</li>
 *   <li>{@code DELETE /api/admin/users/{id}} (관리자) — 컨트롤러가
 *       {@code deletedAt}·{@code isActive} 만 바꿨다. <b>개인정보는 그대로 남는다.</b></li>
 * </ul>
 *
 * <p>이용자가 고객센터로 탈퇴를 요청하면 운영자가 후자를 누른다. 그 경우 이름과 이메일이
 * 계속 남아 있으므로 "파기했다" 고 말할 수 없다. 두 경로가 같은 구현을 쓰게 만든다.
 */
@SpringBootTest(
        classes = CareCodeApplication.class,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration",
                "spring.cache.type=none",
                "spring.datasource.url=jdbc:h2:mem:carecode_withdrawal;MODE=MySQL;DB_CLOSE_DELAY=-1",
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
@DisplayName("탈퇴 처리 일관성")
class AccountWithdrawalConsistencyTest {

    @MockBean RedisConnectionFactory redisConnectionFactory;
    @MockBean StringRedisTemplate stringRedisTemplate;
    @MockBean JavaMailSender javaMailSender;

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired UserRepository userRepository;

    private User admin;
    private User member;
    private String originalEmail;

    @BeforeEach
    void setUp() {
        admin = saveUser(UserRole.ADMIN);
        member = saveUser(UserRole.PARENT);
        originalEmail = member.getEmail();
    }

    @Test
    @DisplayName("관리자가 처리한 탈퇴도 개인정보를 파기한다")
    void adminWithdrawalErasesPersonalData() throws Exception {
        int status = mockMvc.perform(delete("/api/admin/users/{id}", member.getId())
                        .header("Authorization", "Bearer " + token(admin)))
                .andReturn().getResponse().getStatus();

        assertThat(status).isEqualTo(204);

        User after = userRepository.findById(member.getId()).orElseThrow();

        assertThat(after.getDeletedAt()).as("탈퇴 시각").isNotNull();
        assertThat(after.getIsActive()).as("비활성화").isFalse();

        // 본인 탈퇴와 같아야 한다. 남아 있으면 "파기했다" 고 말할 수 없다.
        assertThat(after.getEmail()).as("이메일").isNotEqualTo(originalEmail);
        assertThat(after.getName()).as("이름").isEqualTo("탈퇴한 사용자");
        assertThat(after.getPhoneNumber()).as("전화번호").isNull();
        assertThat(after.getAddress()).as("주소").isNull();
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
                .phoneNumber("010-1234-5678")
                .address("서울특별시 성동구")
                .role(role)
                .isActive(true)
                .emailVerified(true)
                .registrationCompleted(true)
                .createdAt(LocalDateTime.now())
                .build());
    }
}
