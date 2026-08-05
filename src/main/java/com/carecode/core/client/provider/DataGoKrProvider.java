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

/** 공공데이터포털(data.go.kr) 공급자. */
@Slf4j
@Component
public class DataGoKrProvider implements PublicDataProvider {

    public static final String PROVIDER_NAME = "DATA_GO_KR";

    private final RestTemplate restTemplate;
    private final String serviceKey;
    private final String baseUrl;

    public DataGoKrProvider(RestTemplate restTemplate,
                            @Value("${public.data.datagokr.service-key:}") String serviceKey,
                            @Value("${public.data.datagokr.base-url:https://apis.data.go.kr}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.serviceKey = serviceKey;
        this.baseUrl = stripTrailingSlash(baseUrl);
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

        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(toAbsoluteUrl(resource))
                .queryParam("pageNo", pageNo)
                .queryParam("numOfRows", numOfRows);

        // 응답 포맷 파라미터는 데이터셋마다 이름이 다르다(type / _type). 호출부가 지정하지 않았을 때만 기본값을 넣는다 — 지정했는데 또 넣으면 파라미터가 중복된다.
        boolean formatSpecified = params != null
                && (params.containsKey("type") || params.containsKey("_type"));
        if (!formatSpecified) {
            builder.queryParam("type", "json");
        }

        if (params != null) {
            params.forEach((k, v) -> {
                if (v != null && !v.isBlank()) {
                    builder.queryParam(k, v);
                }
            });
        }

        // serviceKey 는 이미 인코딩된 값이므로 빌더를 거치지 않고 직접 이어 붙인다.
        String encoded = builder.encode(StandardCharsets.UTF_8).toUriString();
        String url = encoded + "&serviceKey=" + serviceKey;

        log.debug("공공데이터포털 호출: resource={}, page={}, rows={}", resource, pageNo, numOfRows);

        try {
            return restTemplate.getForObject(URI.create(url), String.class);
        } catch (Exception e) {
            throw new PublicDataApiException(
                    "공공데이터포털 호출 실패: resource=" + resource + ", 사유=" + e.getMessage(), e);
        }
    }

    /** 표준데이터처럼 다른 호스트에 있는 데이터셋은 절대 URL 로 지정할 수 있게 한다. */
    private String toAbsoluteUrl(String resource) {
        if (resource != null && (resource.startsWith("http://") || resource.startsWith("https://"))) {
            return resource;
        }
        return baseUrl + "/" + stripLeadingSlash(resource);
    }

    private String stripTrailingSlash(String url) {
        return url != null && url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private String stripLeadingSlash(String path) {
        return path != null && path.startsWith("/") ? path.substring(1) : path;
    }
}
