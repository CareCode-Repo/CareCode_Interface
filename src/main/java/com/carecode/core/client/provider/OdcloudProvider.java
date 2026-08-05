package com.carecode.core.client.provider;

import com.carecode.core.client.exception.PublicDataApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 공공데이터포털 오픈API(api.odcloud.kr) 공급자.
 * apis.data.go.kr 과 같은 인증키를 쓰지만 페이징 규약이 page/perPage 로 다르다.
 */
@Slf4j
@Component
public class OdcloudProvider implements PublicDataProvider {

    public static final String PROVIDER_NAME = "ODCLOUD";

    private final RestTemplate restTemplate;
    private final String serviceKey;
    private final String baseUrl;

    public OdcloudProvider(RestTemplate restTemplate,
                           @Value("${public.data.datagokr.service-key:}") String serviceKey,
                           @Value("${public.data.odcloud.base-url:https://api.odcloud.kr}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.serviceKey = serviceKey;
        this.baseUrl = baseUrl != null && baseUrl.endsWith("/")
                ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public boolean isAvailable() {
        return serviceKey != null && !serviceKey.isBlank();
    }

    @Override
    public String fetch(String resource, int pageNo, int numOfRows, Map<String, String> params) {
        if (!isAvailable()) {
            throw new PublicDataApiException("공공데이터포털 서비스 키가 설정되지 않았습니다.");
        }

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(toAbsoluteUrl(resource))
                .queryParam("page", pageNo)
                .queryParam("perPage", numOfRows);

        if (params != null) {
            params.forEach((k, v) -> {
                if (v != null && !v.isBlank()) {
                    builder.queryParam(k, v);
                }
            });
        }

        // serviceKey 는 이미 인코딩된 값이라 빌더를 거치면 이중 인코딩된다.
        String url = builder.encode(StandardCharsets.UTF_8).toUriString() + "&serviceKey=" + serviceKey;
        log.debug("odcloud 호출: resource={}, page={}, perPage={}", resource, pageNo, numOfRows);

        try {
            return restTemplate.getForObject(URI.create(url), String.class);
        } catch (Exception e) {
            throw new PublicDataApiException(
                    "odcloud 호출 실패: resource=" + resource + ", 사유=" + e.getMessage(), e);
        }
    }

    private String toAbsoluteUrl(String resource) {
        if (resource != null && (resource.startsWith("http://") || resource.startsWith("https://"))) {
            return resource;
        }
        return baseUrl + "/" + (resource != null && resource.startsWith("/") ? resource.substring(1) : resource);
    }
}
