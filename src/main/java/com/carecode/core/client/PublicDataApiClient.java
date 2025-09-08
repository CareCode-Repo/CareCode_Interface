package com.carecode.core.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

/**
 * 공공데이터 API 호출을 위한 공통 클라이언트
 * 다양한 공공데이터 포털 API 호출에 사용
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PublicDataApiClient {

    private final RestTemplate restTemplate;

    @Value("${public.data.api.key:}")
    private String apiKey;

    @Value("${public.data.api.base-url:}")
    private String baseUrl;

    /**
     * GET 요청으로 공공데이터 API 호출
     * @param endpoint API 엔드포인트
     * @param params 쿼리 파라미터
     * @param responseType 응답 타입
     * @return API 응답
     */
    public <T> T get(String endpoint, Map<String, String> params, Class<T> responseType) {
        // 서울시 API URL 구조: http://openapi.seoul.go.kr:8088/KEY/TYPE/SERVICE/START_INDEX/END_INDEX/
        // endpoint는 SERVICE/START_INDEX/END_INDEX/ 형태로 전달됨
        String url = baseUrl + "/" + apiKey + "/json/" + endpoint;

        log.info("공공데이터 API 호출: {}", url);
        log.info("API 키: {}", apiKey != null ? apiKey.substring(0, Math.min(10, apiKey.length())) + "..." : "null");
        log.info("Base URL: {}", baseUrl);

        ResponseEntity<T> response = restTemplate.getForEntity(url, responseType);

        log.info("공공데이터 API 응답 상태: {}", response.getStatusCode());
        log.info("공공데이터 API 응답 헤더: {}", response.getHeaders());

        if (response.getStatusCode() == HttpStatus.OK) {
            log.info("공공데이터 API 호출 성공: {}", endpoint);
            if (response.getBody() != null) {
                log.debug("응답 본문 길이: {}", response.getBody().toString().length());
            }
            return response.getBody();
        } else {
            log.error("공공데이터 API 호출 실패: {} - {}", endpoint, response.getStatusCode());
            log.error("응답 본문: {}", response.getBody());
            throw new RuntimeException("API 호출 실패: " + response.getStatusCode() + " - " + response.getBody());
        }
    }

    /**
     * POST 요청으로 공공데이터 API 호출
     * @param endpoint API 엔드포인트
     * @param requestBody 요청 본문
     * @param responseType 응답 타입
     * @return API 응답
     */
    public <T> T post(String endpoint, Object requestBody, Class<T> responseType) {
        try {
            String url = baseUrl + endpoint;
            log.info("공공데이터 API POST 호출: {}", url);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("serviceKey", apiKey);

            HttpEntity<Object> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<T> response = restTemplate.postForEntity(url, entity, responseType);

            if (response.getStatusCode() == HttpStatus.OK) {
                log.info("공공데이터 API POST 호출 성공: {}", endpoint);
                return response.getBody();
            } else {
                log.error("공공데이터 API POST 호출 실패: {} - {}", endpoint, response.getStatusCode());
                throw new RuntimeException("API POST 호출 실패: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("공공데이터 API POST 호출 중 오류 발생: {} - {}", endpoint, e.getMessage(), e);
            throw new RuntimeException("API POST 호출 중 오류 발생", e);
        }
    }

    /**
     * 헤더를 포함한 GET 요청
     * @param endpoint API 엔드포인트
     * @param params 쿼리 파라미터
     * @param headers 추가 헤더
     * @param responseType 응답 타입
     * @return API 응답
     */
    public <T> T getWithHeaders(String endpoint, Map<String, String> params, 
                               Map<String, String> headers, Class<T> responseType) {
        try {
            UriComponentsBuilder builder = UriComponentsBuilder
                    .fromHttpUrl(baseUrl + endpoint)
                    .queryParam("serviceKey", apiKey);

            if (params != null) {
                params.forEach(builder::queryParam);
            }

            String url = builder.toUriString();
            log.info("공공데이터 API 헤더 포함 호출: {}", url);

            HttpHeaders httpHeaders = new HttpHeaders();
            if (headers != null) {
                headers.forEach(httpHeaders::add);
            }

            HttpEntity<String> entity = new HttpEntity<>(httpHeaders);
            ResponseEntity<T> response = restTemplate.exchange(url, HttpMethod.GET, entity, responseType);

            if (response.getStatusCode() == HttpStatus.OK) {
                log.info("공공데이터 API 헤더 포함 호출 성공: {}", endpoint);
                return response.getBody();
            } else {
                log.error("공공데이터 API 헤더 포함 호출 실패: {} - {}", endpoint, response.getStatusCode());
                throw new RuntimeException("API 헤더 포함 호출 실패: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("공공데이터 API 헤더 포함 호출 중 오류 발생: {} - {}", endpoint, e.getMessage(), e);
            throw new RuntimeException("API 헤더 포함 호출 중 오류 발생", e);
        }
    }

    /**
     * API 키 설정
     * @param apiKey API 키
     */
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    /**
     * 기본 URL 설정
     * @param baseUrl 기본 URL
     */
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
}