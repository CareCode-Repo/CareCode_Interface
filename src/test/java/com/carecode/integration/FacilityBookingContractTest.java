package com.carecode.integration;

import com.carecode.CareCodeApplication;
import com.carecode.domain.careFacility.entity.CareFacility;
import com.carecode.domain.careFacility.entity.FacilityType;
import com.carecode.domain.careFacility.repository.CareFacilityRepository;
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
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * 시설 리뷰·예약 API 를 실제 JWT 로 호출해 본다.
 *
 * <p>JWT 필터는 principal 로 이메일 문자열을 넣는다. 컨트롤러가 {@code @AuthenticationPrincipal UserDetails}
 * 를 받고 있어 값이 늘 null 이었고, 리뷰 작성·수정·삭제와 예약 생성·조회·취소가 전부 500 이었다.
 * {@code @WithMockUser} 는 principal 을 UserDetails 로 넣어 주므로 이 결함을 재현하지 못한다.
 * 그래서 여기서는 실제 토큰을 발급해 필터를 통과시킨다.
 */
@SpringBootTest(
        classes = CareCodeApplication.class,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration",
                "spring.cache.type=none",
                "spring.datasource.url=jdbc:h2:mem:carecode_facility_contract;MODE=MySQL;DB_CLOSE_DELAY=-1",
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
@DisplayName("시설 리뷰·예약 계약")
class FacilityBookingContractTest {

    @MockBean RedisConnectionFactory redisConnectionFactory;
    @MockBean StringRedisTemplate stringRedisTemplate;
    @MockBean JavaMailSender javaMailSender;

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired UserRepository userRepository;
    @Autowired CareFacilityRepository careFacilityRepository;
    @Autowired ObjectMapper objectMapper;

    private User owner;
    private User stranger;
    private Long facilityId;

    @BeforeEach
    void setUp() {
        owner = saveUser();
        stranger = saveUser();
        facilityId = careFacilityRepository.save(CareFacility.builder()
                .facilityCode("F-" + UUID.randomUUID())
                .name("행복 어린이집")
                .facilityType(FacilityType.DAYCARE)
                .address("서울특별시 강남구")
                .capacity(10)
                .isActive(true)
                .build()).getId();
    }

    @Test
    @DisplayName("리뷰 작성·수정·삭제가 500 없이 동작한다")
    void reviewLifecycle() throws Exception {
        MvcResult created = mockMvc.perform(as(owner, post("/facilities/{id}/reviews", facilityId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":5,\"content\":\"선생님들이 친절해요\"}"))
                .andReturn();
        assertThat(created.getResponse().getStatus()).as(body(created)).isEqualTo(200);
        long reviewId = json(created).path("reviewId").asLong();

        MvcResult updated = mockMvc.perform(as(owner, put("/facilities/reviews/{id}", reviewId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":4,\"content\":\"수정\"}"))
                .andReturn();
        assertThat(updated.getResponse().getStatus()).as(body(updated)).isEqualTo(200);

        MvcResult deleted = mockMvc.perform(as(owner, delete("/facilities/reviews/{id}", reviewId))).andReturn();
        assertThat(deleted.getResponse().getStatus()).as(body(deleted)).isEqualTo(200);
    }

    @Test
    @DisplayName("예약은 본인만 조회·취소할 수 있고, 남의 예약은 403 이다")
    void bookingOwnership() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusDays(3).truncatedTo(ChronoUnit.HOURS);
        String request = """
                {"childName":"아이","childAge":3,"parentName":"보호자","parentPhone":"010-1234-5678",
                 "bookingType":"VISIT","startTime":"%s","endTime":"%s"}
                """.formatted(start, start.plusHours(1));

        MvcResult created = mockMvc.perform(as(owner, post("/facilities/{id}/bookings", facilityId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andReturn();
        assertThat(created.getResponse().getStatus()).as(body(created)).isEqualTo(200);
        long bookingId = json(created).path("id").asLong();

        MvcResult mine = mockMvc.perform(as(owner, get("/facilities/bookings/user"))).andReturn();
        assertThat(mine.getResponse().getStatus()).as(body(mine)).isEqualTo(200);
        assertThat(json(mine)).hasSize(1);

        assertThat(status(as(owner, get("/facilities/bookings/{id}", bookingId)))).isEqualTo(200);
        assertThat(status(as(stranger, get("/facilities/bookings/{id}", bookingId)))).isEqualTo(403);
        assertThat(status(as(stranger, delete("/facilities/bookings/{id}", bookingId)))).isEqualTo(403);
        assertThat(status(as(owner, delete("/facilities/bookings/{id}", bookingId)))).isEqualTo(200);
    }

    @Test
    @DisplayName("다른 사람의 예약(보호자 이름·연락처)이 담긴 목록과 상태 변경은 일반 회원에게 403 이다")
    void facilityWideBookingViewsAreAdminOnly() throws Exception {
        assertThat(status(as(owner, get("/facilities/{id}/bookings", facilityId)))).isEqualTo(403);
        assertThat(status(as(owner, get("/facilities/{id}/bookings/today", facilityId)))).isEqualTo(403);
        assertThat(status(as(owner, get("/facilities/bookings/today")))).isEqualTo(403);
        assertThat(status(as(owner, put("/facilities/bookings/1/status").param("status", "CONFIRMED")))).isEqualTo(403);
    }

    @Test
    @DisplayName("시설 상세·리뷰·검색은 로그인 없이 볼 수 있다")
    void facilityReadsArePublic() throws Exception {
        assertThat(status(get("/facilities/{id}", facilityId))).isEqualTo(200);
        assertThat(status(get("/facilities/{id}/reviews", facilityId))).isEqualTo(200);
        assertThat(status(get("/facilities/{id}/with-reviews", facilityId))).isEqualTo(200);
        assertThat(status(post("/facilities/search").contentType(MediaType.APPLICATION_JSON).content("{}")))
                .isEqualTo(200);
    }

    @Test
    @DisplayName("시설 검색의 유형 필터가 실제로 적용된다")
    void searchAppliesFacilityType() throws Exception {
        MvcResult daycare = mockMvc.perform(post("/facilities/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"facilityType\":\"DAYCARE\",\"size\":50}"))
                .andReturn();
        assertThat(daycare.getResponse().getStatus()).as(body(daycare)).isEqualTo(200);
        assertThat(json(daycare).path("facilities")).isNotEmpty();

        MvcResult kindergarten = mockMvc.perform(post("/facilities/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"facilityType\":\"KINDERGARTEN\",\"size\":50}"))
                .andReturn();
        assertThat(json(kindergarten).path("facilities").findValuesAsText("facilityType"))
                .doesNotContain("DAYCARE");

        assertThat(status(post("/facilities/search").contentType(MediaType.APPLICATION_JSON)
                .content("{\"facilityType\":\"NOPE\"}"))).isEqualTo(400);
    }

    /**
     * {@code @ValidateLocation} 은 인자 toString 에 "latitude" 글자가 있는지를 봐서,
     * 붙은 API 가 입력과 무관하게 전부 400 이었다. 접근제어 테스트는 401·403 만 아니면 통과라 놓쳤다.
     */
    @Test
    @DisplayName("위치 검증이 붙은 조회 API 가 정상 입력에 200 을 준다")
    void locationValidatedEndpointsAcceptValidInput() throws Exception {
        assertThat(status(get("/facilities/radius")
                .param("latitude", "37.5").param("longitude", "127.0").param("radius", "5"))).isEqualTo(200);
        assertThat(status(get("/facilities/location/{location}", "강남구"))).isEqualTo(200);
        assertThat(status(as(owner, get("/policies/location/{location}", "서울")))).isEqualTo(200);

        assertThat(status(get("/facilities/radius")
                .param("latitude", "999").param("longitude", "127.0").param("radius", "5"))).isEqualTo(400);
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

    private int status(MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request).andReturn().getResponse().getStatus();
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
    }

    private static String body(MvcResult result) throws Exception {
        return result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
    }
}
