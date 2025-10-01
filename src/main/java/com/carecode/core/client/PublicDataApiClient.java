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
        try {
            UriComponentsBuilder builder = UriComponentsBuilder
                    .fromHttpUrl(baseUrl + endpoint)
                    .queryParam("serviceKey", apiKey);

            if (params != null) {
                params.forEach(builder::queryParam);
            }

            String url = builder.toUriString();
            log.info("공공데이터 API 호출: {}", url);

            ResponseEntity<T> response = restTemplate.getForEntity(url, responseType);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                log.info("공공데이터 API 호출 성공: {}", endpoint);
                return response.getBody();
            } else {
                log.error("공공데이터 API 호출 실패: {} - {}", endpoint, response.getStatusCode());
                throw new RuntimeException("API 호출 실패: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("공공데이터 API 호출 중 오류 발생: {} - {}", endpoint, e.getMessage(), e);
            throw new RuntimeException("API 호출 중 오류 발생", e);
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
            HttpHeaders headers = new HttpHeaders();

            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("serviceKey", apiKey);

            HttpEntity<Object> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<T> response = restTemplate.postForEntity(url, entity, responseType);

            if (response.getStatusCode() == HttpStatus.OK) {
                return response.getBody();
            } else {
                throw new RuntimeException("API POST 호출 실패: " + response.getStatusCode());
            }
        } catch (Exception e) {
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
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(baseUrl + endpoint)
                .queryParam("serviceKey", apiKey);

        if (params != null) {
            params.forEach(builder::queryParam);
        }

        String url = builder.toUriString();
        HttpHeaders httpHeaders = new HttpHeaders();
        if (headers != null) {
            headers.forEach(httpHeaders::add);
        }

        HttpEntity<String> entity = new HttpEntity<>(httpHeaders);
        ResponseEntity<T> response = restTemplate.exchange(url, HttpMethod.GET, entity, responseType);

        if (response.getStatusCode() == HttpStatus.OK) {
            return response.getBody();
        } else {
            throw new RuntimeException("API 헤더 포함 호출 실패: " + response.getStatusCode());
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