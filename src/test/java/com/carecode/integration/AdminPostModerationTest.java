package com.carecode.integration;

import com.carecode.CareCodeApplication;
import com.carecode.domain.community.entity.Comment;
import com.carecode.domain.community.entity.Post;
import com.carecode.domain.community.entity.PostCategory;
import com.carecode.domain.community.entity.PostStatus;
import com.carecode.domain.community.repository.CommentRepository;
import com.carecode.domain.community.repository.PostRepository;
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
 * 관리자의 게시글 조치는 한 가지여야 한다.
 *
 * <p>같은 의도("이 글은 부적절하다")에 두 결과가 있었다.
 *
 * <ul>
 *   <li>신고 수락({@code ModerationService.resolve} → {@code hideTarget}) — <b>숨김.</b>
 *       {@code isActive=false}, {@code status=HIDDEN}. 행이 남아 댓글·좋아요가 가리키는
 *       대상이 유지되고, 잘못된 조치는 되돌릴 수 있다.</li>
 *   <li>{@code DELETE /api/admin/community/posts/{id}} — <b>하드 삭제.</b> 컨트롤러가
 *       리포지토리에 직접 {@code delete} 를 불렀다. FK 가 전부 {@code ON DELETE CASCADE} 라
 *       <b>댓글·좋아요·북마크까지 함께 사라지고</b> 되돌릴 수 없다.</li>
 * </ul>
 *
 * <p>이 저장소는 사용자 탈퇴에서는 반대 원칙을 적어 두었다 — "하드 삭제하면 게시글·건강기록 등
 * 참조 데이터가 함께 사라지므로 soft delete 로 처리한다"({@code AdminUserController}).
 * 게시글에도 같은 원칙을 적용한다.
 */
@SpringBootTest(
        classes = CareCodeApplication.class,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration",
                "spring.cache.type=none",
                "spring.datasource.url=jdbc:h2:mem:carecode_admin_post;MODE=MySQL;DB_CLOSE_DELAY=-1",
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
@DisplayName("어드민 게시글 모더레이션")
class AdminPostModerationTest {

    @MockBean RedisConnectionFactory redisConnectionFactory;
    @MockBean StringRedisTemplate stringRedisTemplate;
    @MockBean JavaMailSender javaMailSender;

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired UserRepository userRepository;
    @Autowired PostRepository postRepository;
    @Autowired CommentRepository commentRepository;

    private User admin;
    private Post post;
    private Comment comment;

    @BeforeEach
    void setUp() {
        admin = saveUser(UserRole.ADMIN);
        User author = saveUser(UserRole.PARENT);

        post = postRepository.save(Post.builder()
                .title("부적절한 글")
                .content("내용")
                .author(author)
                .category(PostCategory.GENERAL)
                .createdAt(LocalDateTime.now())
                .build());

        comment = commentRepository.save(Comment.builder()
                .post(post)
                .author(author)
                .content("댓글")
                .build());
    }

    @Test
    @DisplayName("관리자 조치는 숨김이다 — 댓글까지 지우지 않는다")
    void adminActionHidesInsteadOfHardDeleting() throws Exception {
        int status = mockMvc.perform(delete("/api/admin/community/posts/{id}", post.getId())
                        .header("Authorization", "Bearer " + token(admin)))
                .andReturn().getResponse().getStatus();

        assertThat(status).isEqualTo(204);

        Post after = postRepository.findById(post.getId()).orElse(null);
        assertThat(after).as("행이 남아야 신고 이력과 댓글이 가리키는 대상이 유지된다").isNotNull();
        assertThat(after.getIsActive()).as("isActive").isFalse();
        assertThat(after.getStatus()).as("status").isEqualTo(PostStatus.HIDDEN);

        assertThat(commentRepository.findById(comment.getId()))
                .as("댓글은 CASCADE 로 함께 사라지면 안 된다")
                .isPresent();
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
