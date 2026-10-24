package com.carecode.integration;

import com.carecode.CareCodeApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * 관제 화면의 경계를 고정한다.
 *
 * <p>껍데기(정적 HTML/JS)는 공개다 — 로그인 전에는 아무 수치도 담고 있지 않다. 수치는 전부
 * 브라우저가 관리자 토큰으로 {@code /api/admin/**} 을 불러 채우고, 그쪽이 ADMIN 을 요구한다.
 *
 * <p>이 테스트가 지키는 것은 <b>그 분리</b>다. 껍데기가 막히면 로그인조차 할 수 없고, 수치
 * 경로가 열리면 관제 화면이 인증 없이 읽히는 창구가 된다.
 */
@SpringBootTest(
        classes = CareCodeApplication.class,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration",
                "spring.cache.type=none",
                "spring.datasource.url=jdbc:h2:mem:carecode_control_plane;MODE=MySQL;DB_CLOSE_DELAY=-1",
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
@DisplayName("관제 화면")
class ControlPlaneTest {

    @MockBean RedisConnectionFactory redisConnectionFactory;
    @MockBean StringRedisTemplate stringRedisTemplate;
    @MockBean JavaMailSender javaMailSender;

    @Autowired MockMvc mockMvc;

    @Test
    @DisplayName("껍데기는 로그인 없이 열린다 - 막히면 로그인조차 할 수 없다")
    void shellIsReachableWithoutLogin() throws Exception {
        MvcResult result = mockMvc.perform(get("/control-plane/index.html")).andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(200);

        String html = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(html).as("관제 화면이 맞는지").contains("CareCode 관제");
    }

    /**
     * 껍데기에 수치가 들어 있으면 공개로 둔 의미가 없다.
     *
     * <p>서버가 아무것도 끼워 넣지 않는다는 것을 <b>파일과 응답을 그대로 비교해</b> 확인한다.
     * "템플릿 문법이 없다" 로 보려 했더니 자바스크립트의 템플릿 리터럴({@code `${...}`})에
     * 걸렸다 — 그건 서버 렌더링과 무관하다. 바이트가 같으면 끼워 넣을 자리가 없다는 뜻이다.
     *
     * <p>회원 수·동기화 시각 같은 값은 브라우저가 로그인 뒤에 불러 채운다.
     */
    @Test
    @DisplayName("껍데기는 서버가 아무것도 끼워 넣지 않은 정적 파일이다")
    void shellIsServedVerbatim() throws Exception {
        String served = mockMvc.perform(get("/control-plane/index.html"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        String onDisk = new String(
                new ClassPathResource("static/control-plane/index.html").getInputStream().readAllBytes(),
                StandardCharsets.UTF_8);

        assertThat(served).isEqualTo(onDisk);
    }

    @ParameterizedTest(name = "{0} 은 로그인 없이 열리지 않는다")
    @ValueSource(strings = {
            "/api/admin/dashboard",
            "/api/admin/sync/status",
            "/api/admin/policies/verification-status"
    })
    void dataEndpointsRequireLogin(String path) throws Exception {
        assertThat(mockMvc.perform(get(path)).andReturn().getResponse().getStatus())
                .as("%s 가 열려 있으면 관제 화면이 인증 없이 읽히는 창구가 된다", path)
                .isIn(401, 403);
    }

    /**
     * 예측 정확도는 공개 지표다. 관제 화면도 같은 경로를 쓴다 — 같은 숫자를 두 곳에서 다르게
     * 계산하지 않기 위해서다.
     */
    @Test
    @DisplayName("예측 정확도는 공개 경로를 그대로 쓴다")
    void forecastAccuracyStaysPublic() throws Exception {
        assertThat(mockMvc.perform(get("/facilities/forecast-accuracy"))
                .andReturn().getResponse().getStatus())
                .isNotIn(401, 403);
    }
}
