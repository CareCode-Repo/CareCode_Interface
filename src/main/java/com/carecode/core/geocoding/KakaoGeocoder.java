package com.carecode.core.geocoding;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/** 카카오 로컬 API 주소 검색. */
@Slf4j
@Component
public class KakaoGeocoder implements Geocoder {

    private static final String SEARCH_URL = "https://dapi.kakao.com/v2/local/search/address.json";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String restApiKey;

    public KakaoGeocoder(RestTemplate restTemplate,
                         ObjectMapper objectMapper,
                         @Value("${app.geocoding.kakao.rest-api-key:}") String restApiKey) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.restApiKey = restApiKey;
        if (restApiKey == null || restApiKey.isBlank()) {
            log.info("카카오 지오코딩 키가 없어 좌표 보정을 건너뜁니다.");
        }
    }

    @Override
    public String getProviderName() {
        return "KAKAO";
    }

    @Override
    public boolean isAvailable() {
        return restApiKey != null && !restApiKey.isBlank();
    }

    @Override
    public Optional<Coordinates> geocode(String address) {
        if (!isAvailable() || address == null || address.isBlank()) {
            return Optional.empty();
        }

        try {
            String url = UriComponentsBuilder.fromHttpUrl(SEARCH_URL)
                    .queryParam("query", address.trim())
                    .queryParam("size", 1)
                    .encode(StandardCharsets.UTF_8)
                    .toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.AUTHORIZATION, "KakaoAK " + restApiKey);

            ResponseEntity<String> response = restTemplate.exchange(
                    URI.create(url), org.springframework.http.HttpMethod.GET,
                    new HttpEntity<>(headers), String.class);

            return parse(response.getBody());
        } catch (Exception e) {
            // 한 건 실패가 배치를 멈추면 안 된다.
            log.debug("지오코딩 실패 - address={}, 사유={}", address, e.getMessage());
            return Optional.empty();
        }
    }

    /** 응답의 x 가 경도, y 가 위도다. 뒤집으면 지도에서 엉뚱한 곳이 나온다. */
    private Optional<Coordinates> parse(String body) throws Exception {
        if (body == null || body.isBlank()) {
            return Optional.empty();
        }
        JsonNode documents = objectMapper.readTree(body).path("documents");
        if (!documents.isArray() || documents.isEmpty()) {
            return Optional.empty();
        }
        JsonNode first = documents.get(0);
        double lng = first.path("x").asDouble(0);
        double lat = first.path("y").asDouble(0);

        Coordinates coordinates = new Coordinates(lat, lng);
        return coordinates.isWithinKorea() ? Optional.of(coordinates) : Optional.empty();
    }
}
