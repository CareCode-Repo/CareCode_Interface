package com.carecode.core.geocoding;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.never;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@DisplayName("카카오 지오코딩")
class KakaoGeocoderTest {

    private static final String KEY = "testRestApiKey";

    /** 카카오 응답은 x 가 경도, y 가 위도다. */
    private static final String RESPONSE = """
            {"documents":[{"address_name":"서울 종로구 자하문로 69","x":"126.9662","y":"37.5806"}]}
            """;

    @Test
    @DisplayName("키가 없으면 비활성 상태로 아무것도 호출하지 않는다")
    void inactiveWithoutKey() {
        RestTemplate rest = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(rest).build();
        KakaoGeocoder geocoder = new KakaoGeocoder(rest, new ObjectMapper(), "");

        server.expect(never(), request -> { });

        assertThat(geocoder.isAvailable()).isFalse();
        assertThat(geocoder.geocode("서울시청")).isEmpty();
        server.verify();
    }

    @Test
    @DisplayName("x 를 경도, y 를 위도로 읽는다")
    void mapsXToLongitudeAndYToLatitude() {
        RestTemplate rest = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(rest).build();
        KakaoGeocoder geocoder = new KakaoGeocoder(rest, new ObjectMapper(), KEY);

        server.expect(header("Authorization", "KakaoAK " + KEY))
                .andRespond(withSuccess(RESPONSE, MediaType.APPLICATION_JSON));

        Optional<Geocoder.Coordinates> result = geocoder.geocode("서울특별시 종로구 자하문로 69");

        assertThat(result).isPresent();
        // 뒤집히면 위도 126 이 되어 지도에서 엉뚱한 곳이 나온다
        assertThat(result.get().latitude()).isEqualTo(37.5806);
        assertThat(result.get().longitude()).isEqualTo(126.9662);
        server.verify();
    }

    @Test
    @DisplayName("검색 결과가 없으면 비어 있는 값을 준다")
    void emptyWhenNoDocuments() {
        RestTemplate rest = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(rest).build();
        KakaoGeocoder geocoder = new KakaoGeocoder(rest, new ObjectMapper(), KEY);

        server.expect(request -> { })
                .andRespond(withSuccess("{\"documents\":[]}", MediaType.APPLICATION_JSON));

        assertThat(geocoder.geocode("존재하지 않는 주소")).isEmpty();
    }

    @Test
    @DisplayName("호출이 실패해도 예외를 던지지 않는다")
    void swallowsFailure() {
        RestTemplate rest = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(rest).build();
        KakaoGeocoder geocoder = new KakaoGeocoder(rest, new ObjectMapper(), KEY);

        server.expect(request -> { }).andRespond(withServerError());

        // 배치가 한 건 때문에 멈추면 안 된다
        assertThat(geocoder.geocode("서울시청")).isEmpty();
    }

    @Test
    @DisplayName("한반도 밖 좌표는 잘못된 변환으로 보고 버린다")
    void rejectsCoordinatesOutsideKorea() {
        RestTemplate rest = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(rest).build();
        KakaoGeocoder geocoder = new KakaoGeocoder(rest, new ObjectMapper(), KEY);

        server.expect(request -> { }).andRespond(withSuccess(
                "{\"documents\":[{\"x\":\"-74.0060\",\"y\":\"40.7128\"}]}", MediaType.APPLICATION_JSON));

        assertThat(geocoder.geocode("New York")).isEmpty();
    }

    @Test
    @DisplayName("빈 주소는 호출하지 않는다")
    void skipsBlankAddress() {
        RestTemplate rest = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(rest).build();
        KakaoGeocoder geocoder = new KakaoGeocoder(rest, new ObjectMapper(), KEY);

        server.expect(never(), request -> { });

        assertThat(geocoder.geocode(null)).isEmpty();
        assertThat(geocoder.geocode("  ")).isEmpty();
        server.verify();
    }
}
