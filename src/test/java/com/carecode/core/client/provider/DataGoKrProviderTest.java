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

/** 공공데이터포털 공급자 검증. */
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
    @DisplayName("절대 URL 리소스는 기본 호스트를 붙이지 않는다")
    void usesAbsoluteResourceUrlAsIs() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        DataGoKrProvider provider = new DataGoKrProvider(restTemplate, ENCODED_KEY, "https://apis.data.go.kr");

        server.expect(request -> {
                    URI uri = request.getURI();
                    // 표준데이터는 별도 호스트에 있다
                    assertThat(uri.getHost()).isEqualTo("api.data.go.kr");
                    assertThat(uri.getPath()).isEqualTo("/openapi/tn_pubr_public_kindergarten_api");
                })
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        provider.fetch("http://api.data.go.kr/openapi/tn_pubr_public_kindergarten_api", 1, 10, Map.of());

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
