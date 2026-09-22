package com.carecode.integration;

import com.carecode.CareCodeApplication;
import com.carecode.domain.policy.entity.Policy;
import com.carecode.domain.policy.repository.PolicyRepository;
import com.carecode.domain.user.entity.User;
import com.carecode.domain.user.entity.UserRole;
import com.carecode.domain.user.repository.UserRepository;
import com.carecode.domain.user.service.JwtService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * 프런트가 실제로 보내고 읽는 JSON 모양 그대로 커뮤니티·정책 API 를 호출해 본다.
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
                "spring.datasource.url=jdbc:h2:mem:carecode_content_contract;MODE=MySQL;DB_CLOSE_DELAY=-1",
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
@DisplayName("커뮤니티·정책 응답 계약")
class CommunityPolicyContractTest {

    @MockBean RedisConnectionFactory redisConnectionFactory;
    @MockBean StringRedisTemplate stringRedisTemplate;
    @MockBean JavaMailSender javaMailSender;

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired UserRepository userRepository;
    @Autowired PolicyRepository policyRepository;
    @Autowired ObjectMapper objectMapper;

    private User author;
    private User reader;

    @BeforeEach
    void setUp() {
        author = saveUser();
        reader = saveUser();
    }

    /**
     * 프런트는 {@code isAnonymous} 로 보낸다. 서버 DTO 의 primitive {@code boolean isAnonymous} 는
     * JSON 키가 {@code anonymous} 라 값이 버려졌고, 익명으로 쓴 글이 실명으로 올라갔다.
     * 게다가 익명 글도 응답에 실명과 작성자 ID 가 그대로 실려 있었다.
     */
    @Test
    @DisplayName("익명 글은 익명으로 저장되고, 남에게는 이름과 작성자 ID 가 가려진다")
    void anonymousPostHidesAuthor() throws Exception {
        long postId = createPost(true);

        JsonNode asReader = json(mockMvc.perform(as(reader, get("/community/posts/{id}", postId))).andReturn());
        assertThat(asReader.path("isAnonymous").asBoolean()).isTrue();
        assertThat(asReader.path("authorName").asText()).isEqualTo("익명");
        assertThat(asReader.path("authorId").asText()).isEmpty();

        JsonNode anonymousViewer = json(mockMvc.perform(get("/community/posts/{id}", postId)).andReturn());
        assertThat(anonymousViewer.path("authorName").asText()).isEqualTo("익명");
        assertThat(anonymousViewer.path("authorId").asText()).isEmpty();

        // 작성자 본인은 수정·삭제 버튼을 띄워야 하므로 ID 를 받는다.
        JsonNode asAuthor = json(mockMvc.perform(as(author, get("/community/posts/{id}", postId))).andReturn());
        assertThat(asAuthor.path("authorId").asText()).isEqualTo(author.getId().toString());

        JsonNode list = json(mockMvc.perform(get("/community/posts")).andReturn());
        for (JsonNode post : list.path("content")) {
            if (post.path("postId").asLong() == postId) {
                assertThat(post.path("authorName").asText()).isEqualTo("익명");
                assertThat(post.path("authorId").asText()).isEmpty();
            }
        }
    }

    @Test
    @DisplayName("실명 글은 이름이 그대로 나간다")
    void namedPostShowsAuthor() throws Exception {
        long postId = createPost(false);

        JsonNode post = json(mockMvc.perform(get("/community/posts/{id}", postId)).andReturn());
        assertThat(post.path("isAnonymous").asBoolean()).isFalse();
        assertThat(post.path("authorName").asText()).isEqualTo(author.getName());
        assertThat(post.path("authorId").asText()).isEqualTo(author.getId().toString());
    }

    @Test
    @DisplayName("좋아요를 누르면 좋아요 수와 내 좋아요 여부가 응답에 반영된다")
    void likeIsReflected() throws Exception {
        long postId = createPost(false);

        MvcResult liked = mockMvc.perform(as(reader, post("/community/posts/{id}/like", postId))).andReturn();
        assertThat(liked.getResponse().getStatus()).isEqualTo(200);
        mockMvc.perform(as(reader, post("/community/posts/{id}/bookmark", postId))).andReturn();

        JsonNode asReader = json(mockMvc.perform(as(reader, get("/community/posts/{id}", postId))).andReturn());
        assertThat(asReader.path("likeCount").asInt()).isEqualTo(1);
        assertThat(asReader.path("isLiked").asBoolean()).isTrue();
        assertThat(asReader.path("isBookmarked").asBoolean()).isTrue();

        JsonNode asAuthor = json(mockMvc.perform(as(author, get("/community/posts/{id}", postId))).andReturn());
        assertThat(asAuthor.path("likeCount").asInt()).isEqualTo(1);
        assertThat(asAuthor.path("isLiked").asBoolean()).isFalse();

        // 취소하면 다시 0
        mockMvc.perform(as(reader, post("/community/posts/{id}/like", postId))).andReturn();
        JsonNode after = json(mockMvc.perform(get("/community/posts/{id}", postId)).andReturn());
        assertThat(after.path("likeCount").asInt()).isZero();
    }

    @Test
    @DisplayName("본문 검증이 동작한다 — 빈 제목은 400")
    void postBodyIsValidated() throws Exception {
        MvcResult result = mockMvc.perform(as(author, post("/community/posts"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\",\"content\":\"내용\"}"))
                .andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(400);
    }

    /**
     * 검색만 항목을 PolicyInfoResponse(문자열 id, 다른 필드 이름)로 옮겨 담고 페이지 필드 이름도 달라서
     * 프런트 스키마 파싱이 실패했고 검색 결과 화면은 늘 오류였다.
     */
    @Test
    @DisplayName("정책 검색은 다른 정책 목록과 같은 항목 모양과 프런트가 읽는 페이지 필드를 준다")
    void policySearchShape() throws Exception {
        policyRepository.save(Policy.builder()
                .policyCode("P-" + UUID.randomUUID())
                .title("양육수당 지원")
                .description("가정양육 아동에게 지급")
                .targetRegion("서울특별시")
                .isActive(true)
                .build());

        MvcResult result = mockMvc.perform(post("/policies/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"keyword\":\"양육\",\"location\":\"서울\"}"))
                .andReturn();
        assertThat(result.getResponse().getStatus()).as(result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8)).isEqualTo(200);

        JsonNode body = json(result);
        assertThat(body.path("totalElements").asLong()).isGreaterThanOrEqualTo(1);
        assertThat(body.has("pageSize")).isTrue();
        assertThat(body.has("totalPages")).isTrue();
        assertThat(body.has("currentPage")).isTrue();
        JsonNode first = body.path("policies").get(0);
        assertThat(first.path("id").isNumber()).isTrue();
        assertThat(first.path("title").asText()).contains("양육");
    }

    private long createPost(boolean anonymous) throws Exception {
        MvcResult created = mockMvc.perform(as(author, post("/community/posts"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"제목\",\"content\":\"내용\",\"category\":\"PARENTING\",\"isAnonymous\":%s}"
                                .formatted(anonymous)))
                .andReturn();
        assertThat(created.getResponse().getStatus())
                .as(created.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8)).isEqualTo(200);
        JsonNode body = json(created);
        assertThat(body.path("isAnonymous").asBoolean()).isEqualTo(anonymous);
        return body.path("postId").asLong();
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

    private MockHttpServletRequestBuilder as(User user, MockHttpServletRequestBuilder request) {
        String token = jwtService.generateAccessToken(user.getUserId(), user.getEmail(), user.getRole().name());
        return request.header("Authorization", "Bearer " + token);
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
    }
}
