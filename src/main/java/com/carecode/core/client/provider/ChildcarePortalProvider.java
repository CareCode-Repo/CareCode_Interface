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
 * 보육통합정보시스템(api.childcare.go.kr) 공급자.
 * data.go.kr 과 호스트·인증 파라미터·응답 포맷이 모두 달라 별도 구현이 필요하다.
 */
@Slf4j
@Component
public class ChildcarePortalProvider implements PublicDataProvider {

    public static final String PROVIDER_NAME = "CHILDCARE_PORTAL";

    private final RestTemplate restTemplate;
    private final String serviceKey;
    private final String baseUrl;
    private final String keyParam;
    private final String pageParam;
    private final String sizeParam;

    public ChildcarePortalProvider(
            RestTemplate restTemplate,
            @Value("${public.data.childcare-portal.service-key:}") String serviceKey,
            @Value("${public.data.childcare-portal.base-url:http://api.childcare.go.kr}") String baseUrl,
            // 명세서마다 파라미터명이 달라 설정으로 뺀다. 문서와 다르면 여기만 고치면 된다.
            @Value("${public.data.childcare-portal.key-param:key}") String keyParam,
            @Value("${public.data.childcare-portal.page-param:}") String pageParam,
            @Value("${public.data.childcare-portal.size-param:}") String sizeParam) {
        this.restTemplate = restTemplate;
        this.serviceKey = serviceKey;
        this.baseUrl = stripTrailingSlash(baseUrl);
        this.keyParam = keyParam;
        this.pageParam = pageParam;
        this.sizeParam = sizeParam;
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
            throw new PublicDataApiException("보육통합정보 서비스 키가 설정되지 않았습니다.");
        }

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(toAbsoluteUrl(resource))
                .queryParam(keyParam, serviceKey);

        // 이 API 는 페이징 파라미터가 명세서마다 다르고 없는 경우도 있다. 설정된 경우에만 붙인다.
        if (!pageParam.isBlank()) {
            builder.queryParam(pageParam, pageNo);
        }
        if (!sizeParam.isBlank()) {
            builder.queryParam(sizeParam, numOfRows);
        }
        if (params != null) {
            params.forEach((k, v) -> {
                if (v != null && !v.isBlank()) {
                    builder.queryParam(k, v);
                }
            });
        }

        String url = builder.encode(StandardCharsets.UTF_8).toUriString();
        log.debug("보육통합정보 호출: resource={}, page={}", resource, pageNo);

        try {
            return restTemplate.getForObject(URI.create(url), String.class);
        } catch (Exception e) {
            throw new PublicDataApiException(
                    "보육통합정보 호출 실패: resource=" + resource + ", 사유=" + e.getMessage(), e);
        }
    }

    private String toAbsoluteUrl(String resource) {
        if (resource != null && (resource.startsWith("http://") || resource.startsWith("https://"))) {
            return resource;
        }
        return baseUrl + "/" + (resource != null && resource.startsWith("/") ? resource.substring(1) : resource);
    }

    private String stripTrailingSlash(String url) {
        return url != null && url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
