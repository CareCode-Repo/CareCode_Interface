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
 * 유치원알리미(e-childschoolinfo.moe.go.kr) 공급자.
 * 페이지가 아니라 시군구 단위로 조회하므로 pageNo·numOfRows 를 보내지 않는다.
 */
@Slf4j
@Component
public class KindergartenInfoProvider implements PublicDataProvider {

    public static final String PROVIDER_NAME = "KINDERGARTEN_INFO";

    private final RestTemplate restTemplate;
    private final String serviceKey;
    private final String baseUrl;

    public KindergartenInfoProvider(
            RestTemplate restTemplate,
            @Value("${public.data.kindergarten-info.service-key:}") String serviceKey,
            @Value("${public.data.kindergarten-info.base-url:https://e-childschoolinfo.moe.go.kr}") String baseUrl) {
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
            throw new PublicDataApiException("유치원알리미 서비스 키가 설정되지 않았습니다.");
        }

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(toAbsoluteUrl(resource))
                .queryParam("key", serviceKey);

        if (params != null) {
            params.forEach((k, v) -> {
                if (v != null && !v.isBlank()) {
                    builder.queryParam(k, v);
                }
            });
        }

        String url = builder.encode(StandardCharsets.UTF_8).toUriString();
        log.debug("유치원알리미 호출: {}", params);

        try {
            return restTemplate.getForObject(URI.create(url), String.class);
        } catch (Exception e) {
            throw new PublicDataApiException("유치원알리미 호출 실패: 사유=" + e.getMessage(), e);
        }
    }

    private String toAbsoluteUrl(String resource) {
        if (resource != null && (resource.startsWith("http://") || resource.startsWith("https://"))) {
            return resource;
        }
        return baseUrl + "/" + (resource != null && resource.startsWith("/") ? resource.substring(1) : resource);
    }
}
