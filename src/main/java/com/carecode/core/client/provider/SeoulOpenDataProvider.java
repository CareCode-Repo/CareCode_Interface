package com.carecode.core.client.provider;

import com.carecode.core.client.exception.PublicDataApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * 서울 열린데이터광장 공급자.
 *
 * <p>URL 규격: {@code {baseUrl}/{apiKey}/json/{service}/{startIndex}/{endIndex}/}
 * 페이지 번호가 아니라 시작·종료 인덱스를 경로에 넣는다. 한 번에 최대 1000건.
 */
@Slf4j
@Component
public class SeoulOpenDataProvider implements PublicDataProvider {

    public static final String PROVIDER_NAME = "SEOUL_OPEN_DATA";

    /** 서울시 API가 한 번에 허용하는 최대 건수. */
    private static final int MAX_ROWS = 1000;

    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String baseUrl;

    public SeoulOpenDataProvider(RestTemplate restTemplate,
                                 @Value("${public.data.seoul.api-key:${public.data.api.key:}}") String apiKey,
                                 @Value("${public.data.seoul.base-url:http://openapi.seoul.go.kr:8088}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.apiKey = apiKey;
        this.baseUrl = stripTrailingSlash(baseUrl);
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public boolean isAvailable() {
        return apiKey != null && !apiKey.isBlank();
    }

    @Override
    public String fetch(String resource, int pageNo, int numOfRows, Map<String, String> params) {
        if (!isAvailable()) {
            throw new PublicDataApiException("서울 열린데이터광장 API 키가 설정되지 않았습니다.");
        }

        int rows = Math.min(numOfRows, MAX_ROWS);
        int startIndex = (pageNo - 1) * rows + 1;
        int endIndex = pageNo * rows;

        String url = String.format("%s/%s/json/%s/%d/%d/", baseUrl, apiKey, resource, startIndex, endIndex);
        log.debug("서울 열린데이터광장 호출: service={}, {}~{}", resource, startIndex, endIndex);

        try {
            return restTemplate.getForObject(url, String.class);
        } catch (Exception e) {
            // API 키가 URL 경로에 들어가므로 예외 메시지에 URL 을 넣지 않는다.
            throw new PublicDataApiException(
                    "서울 열린데이터광장 호출 실패: service=" + resource + ", 사유=" + e.getMessage(), e);
        }
    }

    private String stripTrailingSlash(String url) {
        return url != null && url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
