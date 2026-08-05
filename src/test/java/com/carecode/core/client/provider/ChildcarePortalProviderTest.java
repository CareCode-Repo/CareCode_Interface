package com.carecode.core.client.provider;

import com.carecode.core.client.exception.PublicDataApiException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@DisplayName("보육통합정보 공급자")
class ChildcarePortalProviderTest {

    private static final String KEY = "testkey123";
    private static final String URL =
            "http://api.childcare.go.kr/mediate/rest/cpmsapi021/cpmsapi021/request";

    @Test
    @DisplayName("서비스 키가 없으면 비활성 상태다")
    void inactiveWithoutKey() {
        ChildcarePortalProvider provider = provider("", "key", "", "");

        assertThat(provider.isAvailable()).isFalse();
        assertThatThrownBy(() -> provider.fetch(URL, 1, 10, Map.of()))
                .isInstanceOf(PublicDataApiException.class)
                .hasMessageContaining("서비스 키");
    }

    @Test
    @DisplayName("data.go.kr 과 달리 key 파라미터로 인증한다")
    void usesKeyParameter() {
        RestTemplate rest = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(rest).build();
        ChildcarePortalProvider provider = provider(rest, KEY, "key", "", "");

        server.expect(request -> {
            String query = request.getURI().getRawQuery();
            assertThat(query).contains("key=" + KEY);
            assertThat(query).doesNotContain("serviceKey=");
        }).andRespond(withSuccess("<response/>", MediaType.APPLICATION_XML));

        provider.fetch(URL, 1, 100, Map.of());
        server.verify();
    }

    @Test
    @DisplayName("인증 파라미터명을 설정으로 바꿀 수 있다")
    void keyParameterIsConfigurable() {
        RestTemplate rest = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(rest).build();
        // 명세서가 apiKey 를 쓰면 코드가 아니라 설정만 고친다
        ChildcarePortalProvider provider = provider(rest, KEY, "apiKey", "", "");

        server.expect(request ->
                assertThat(request.getURI().getRawQuery()).contains("apiKey=" + KEY))
                .andRespond(withSuccess("<response/>", MediaType.APPLICATION_XML));

        provider.fetch(URL, 1, 100, Map.of());
        server.verify();
    }

    @Test
    @DisplayName("페이징 파라미터는 설정된 경우에만 붙인다")
    void omitsPagingWhenNotConfigured() {
        RestTemplate rest = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(rest).build();
        ChildcarePortalProvider provider = provider(rest, KEY, "key", "", "");

        server.expect(request -> {
            String query = request.getURI().getRawQuery();
            assertThat(query).doesNotContain("pageNo").doesNotContain("numOfRows");
        }).andRespond(withSuccess("<response/>", MediaType.APPLICATION_XML));

        provider.fetch(URL, 3, 500, Map.of());
        server.verify();
    }

    @Test
    @DisplayName("페이징 파라미터를 지정하면 전달한다")
    void sendsPagingWhenConfigured() {
        RestTemplate rest = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(rest).build();
        ChildcarePortalProvider provider = provider(rest, KEY, "key", "pageIndex", "pageUnit");

        server.expect(request -> {
            String query = request.getURI().getRawQuery();
            assertThat(query).contains("pageIndex=3").contains("pageUnit=500");
        }).andRespond(withSuccess("<response/>", MediaType.APPLICATION_XML));

        provider.fetch(URL, 3, 500, Map.of());
        server.verify();
    }

    @Test
    @DisplayName("추가 파라미터를 함께 보낸다")
    void sendsExtraParams() {
        RestTemplate rest = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(rest).build();
        ChildcarePortalProvider provider = provider(rest, KEY, "key", "", "");

        server.expect(request -> {
            URI uri = request.getURI();
            assertThat(uri.getHost()).isEqualTo("api.childcare.go.kr");
            assertThat(uri.getRawQuery()).contains("arcode=11");
        }).andRespond(withSuccess("<response/>", MediaType.APPLICATION_XML));

        provider.fetch(URL, 1, 100, Map.of("arcode", "11"));
        server.verify();
    }

    private ChildcarePortalProvider provider(String key, String keyParam, String page, String size) {
        return provider(new RestTemplate(), key, keyParam, page, size);
    }

    private ChildcarePortalProvider provider(RestTemplate rest, String key, String keyParam,
                                             String page, String size) {
        return new ChildcarePortalProvider(rest, key, "http://api.childcare.go.kr", keyParam, page, size);
    }
}
