package com.carecode.core.ops;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.never;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@DisplayName("운영 알림")
class OperationalAlerterTest {

    private static final String WEBHOOK = "https://hooks.slack.test/abc";

    @Test
    @DisplayName("웹훅이 없으면 비활성 상태로 동작한다")
    void disabledWithoutWebhook() {
        RestTemplate rest = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(rest).build();
        OperationalAlerter alerter = new OperationalAlerter(rest, "", "test");

        server.expect(never(), requestTo(WEBHOOK));
        alerter.alert("key", "제목", "내용");

        assertThat(alerter.isEnabled()).isFalse();
        server.verify();
    }

    @Test
    @DisplayName("웹훅이 있으면 전송한다")
    void sendsWhenConfigured() {
        RestTemplate rest = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(rest).build();
        OperationalAlerter alerter = new OperationalAlerter(rest, WEBHOOK, "prod");

        server.expect(once(), requestTo(WEBHOOK))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("ok", MediaType.TEXT_PLAIN));

        alerter.alert("sync-fail", "동기화 실패", "상세 내용");

        server.verify();
    }

    @Test
    @DisplayName("같은 키는 쿨다운 동안 한 번만 보낸다")
    void suppressesDuplicateAlerts() {
        RestTemplate rest = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(rest).build();
        OperationalAlerter alerter = new OperationalAlerter(rest, WEBHOOK, "prod");

        // 같은 장애로 알림이 쏟아지면 아무도 보지 않게 된다
        server.expect(once(), requestTo(WEBHOOK)).andRespond(withSuccess("ok", MediaType.TEXT_PLAIN));

        alerter.alert("same-key", "제목", "1회차");
        alerter.alert("same-key", "제목", "2회차");
        alerter.alert("same-key", "제목", "3회차");

        server.verify();
    }

    @Test
    @DisplayName("전송 실패가 호출부로 전파되지 않는다")
    void swallowsSendFailure() {
        RestTemplate rest = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(rest).build();
        OperationalAlerter alerter = new OperationalAlerter(rest, WEBHOOK, "prod");

        server.expect(once(), requestTo(WEBHOOK))
                .andRespond(request -> {
                    throw new java.io.IOException("연결 실패");
                });

        alerter.alert("key", "제목", "내용"); // 예외가 밖으로 나오면 안 된다
    }
}
