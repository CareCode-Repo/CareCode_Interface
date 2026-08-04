package com.carecode.core.client.provider;

import com.carecode.core.client.exception.PublicDataApiException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * 공공데이터포털 공급자 검증.
 *
 * <p>serviceKey 는 발급 시점에 이미 URL 인코딩된 문자열이라,
 * 빌더로 다시 인코딩하면 {@code %2B} 가 {@code %252B} 로 이중 인코딩돼 인증에 실패한다.
 */
@DisplayName("DataGoKrProvider")
class DataGoKrProviderTest {

    private static final String ENCODED_KEY = "abc%2Bdef%3D%3D";

    @Test
    @DisplayName("서비스 키가 없으면 비활성 상태다")
    void inactiveWithoutServiceKey() {
        DataGoKrProvider provider = new DataGoKrProvider(new RestTemplate(), "", "https://apis.data.go.kr");

        assertThat(provider.isAvailable()).isFalse();
        assertThatThrownBy(() -> provider.fetch("some/resource", 1, 10, Map.of()))
                .isInstanceOf(PublicDataApiException.class)
                .hasMessageContaining("서비스 키");
    }

    @Test
    @DisplayName("serviceKey를 이중 인코딩하지 않는다")
    void doesNotDoubleEncodeServiceKey() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        DataGoKrProvider provider = new DataGoKrProvider(restTemplate, ENCODED_KEY, "https://apis.data.go.kr");

        server.expect(request -> {
                    URI uri = request.getURI();
                    String query = uri.getRawQuery();
                    assertThat(query).contains("serviceKey=" + ENCODED_KEY);
                    // 이중 인코딩되면 %252B 가 된다
                    assertThat(query).doesNotContain("%252B");
                })
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"response\":{}}", MediaType.APPLICATION_JSON));

        String body = provider.fetch("B551014/CCEF/childcare", 1, 100, Map.of());

        assertThat(body).isEqualTo("{\"response\":{}}");
        server.verify();
    }

    @Test
    @DisplayName("페이지·건수를 쿼리 파라미터로 전달한다")
    void sendsPagingAsQueryParams() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        DataGoKrProvider provider = new DataGoKrProvider(restTemplate, ENCODED_KEY, "https://apis.data.go.kr");

        server.expect(request -> {
                    String query = request.getURI().getRawQuery();
                    assertThat(query).contains("pageNo=3");
                    assertThat(query).contains("numOfRows=50");
                    assertThat(query).contains("sidoCd=11");
                })
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        provider.fetch("some/resource", 3, 50, Map.of("sidoCd", "11"));

        server.verify();
    }

    @Test
    @DisplayName("빈 파라미터 값은 쿼리에서 제외한다")
    void skipsBlankParams() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        DataGoKrProvider provider = new DataGoKrProvider(restTemplate, ENCODED_KEY, "https://apis.data.go.kr");

        server.expect(request -> assertThat(request.getURI().getRawQuery()).doesNotContain("sidoCd"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        java.util.Map<String, String> params = new java.util.HashMap<>();
        params.put("sidoCd", "");
        provider.fetch("some/resource", 1, 10, params);

        server.verify();
    }
}
