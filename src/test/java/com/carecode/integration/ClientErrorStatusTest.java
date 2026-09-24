package com.carecode.integration;

import com.carecode.CareCodeApplication;
import com.carecode.core.ops.OperationalAlerter;
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
import org.springframework.test.web.servlet.RequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * 클라이언트가 잘못 보낸 요청은 4xx 로 끝나고 운영 알림을 울리지 않는다.
 *
 * <p>전역 핸들러에 {@code @ExceptionHandler(Exception.class)} 가 있고, 이 클래스는
 * {@code ResponseEntityExceptionHandler} 를 상속하지 않는다. 그래서 스프링이 원래 4xx 로
 * 바꿔주던 예외(깨진 JSON, 빠진 파라미터, 타입 불일치, 지원하지 않는 메서드)까지 전부
 * 최후 핸들러가 삼켜 <b>500 + Slack 운영 알림</b>이 됐다.
 *
 * <p>운영 문서(operations.md)가 경계하는 바로 그 유형이다 — "봇이 긁고 가면 알림이 울리고,
 * 진짜 장애가 그 소음에 묻힌다". 없는 URL(404)과 권한 거부(403)는 예전에 걸러냈는데
 * 이 네 가지가 남아 있었다.
 */
@SpringBootTest(
        classes = CareCodeApplication.class,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration",
                "spring.cache.type=none",
                "spring.datasource.url=jdbc:h2:mem:carecode_clienterr;MODE=MySQL;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.flyway.enabled=false",
                "jwt.secret=testJwtSecretKeyForClientErrorStatusMustBe256BitsLong0123456789",
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
@DisplayName("클라이언트 오류는 4xx, 알림 없음")
class ClientErrorStatusTest {

    @MockBean RedisConnectionFactory redisConnectionFactory;
    @MockBean StringRedisTemplate stringRedisTemplate;
    @MockBean JavaMailSender javaMailSender;
    @MockBean OperationalAlerter alerter;

    @Autowired MockMvc mockMvc;

    private int status(RequestBuilder request) throws Exception {
        MvcResult result = mockMvc.perform(request).andReturn();
        return result.getResponse().getStatus();
    }

    @Test
    @DisplayName("깨진 JSON 은 400")
    void malformedJson() throws Exception {
        assertThat(status(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content("{not json")))
                .isEqualTo(400);
        verify(alerter, never()).alert(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("필수 파라미터가 빠지면 400")
    void missingParameter() throws Exception {
        assertThat(status(post("/auth/send-code"))).isEqualTo(400);
        verify(alerter, never()).alert(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("경로 변수 타입이 틀리면 400")
    void typeMismatch() throws Exception {
        assertThat(status(get("/policies/not-a-number"))).isEqualTo(400);
        verify(alerter, never()).alert(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("지원하지 않는 메서드는 405")
    void methodNotAllowed() throws Exception {
        assertThat(status(delete("/auth/login"))).isEqualTo(405);
        verify(alerter, never()).alert(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("지원하지 않는 Content-Type 은 415")
    void unsupportedMediaType() throws Exception {
        assertThat(status(post("/auth/login").contentType(MediaType.TEXT_PLAIN).content("x")))
                .isEqualTo(415);
        verify(alerter, never()).alert(anyString(), anyString(), anyString());
    }
}
