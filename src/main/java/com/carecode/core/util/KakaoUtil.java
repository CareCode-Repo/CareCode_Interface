package com.carecode.core.util;

import com.carecode.domain.user.dto.response.KakaoOAuthToken;
import com.carecode.domain.user.dto.response.KakaoProfile;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
@Slf4j
public class KakaoUtil {
    @Value("${spring.security.oauth2.client.registration.kakao.client-id}") 
    private String clientId;
    
    @Value("${spring.security.oauth2.client.registration.kakao.client-secret}") 
    private String clientSecret;
    
    @Value("${kakao.redirect-uri}")
    private String redirectUri;
    
    @Value("${server.port:8081}")
    private String serverPort;
    
    @Value("${server.address:13.209.36.209}")
    private String serverAddress;
    
    // ObjectMapper 설정 - 알 수 없는 필드 무시
    private final ObjectMapper objectMapper = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    /**
     * 카카오 액세스 토큰 요청
     */
    public KakaoOAuthToken requestToken(String accessCode) {
        if (accessCode == null || accessCode.trim().isEmpty()) {
            throw new IllegalArgumentException("인증 코드가 비어있습니다.");
        }
        
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("redirect_uri", redirectUri);
        params.add("code", accessCode);

        HttpEntity<MultiValueMap<String, String>> kakaoTokenRequest = new HttpEntity<>(params, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    "https://kauth.kakao.com/oauth/token", 
                    HttpMethod.POST, 
                    kakaoTokenRequest, 
                    String.class
            );
            
            if (!response.getStatusCode().is2xxSuccessful()) {
                String errorBody = response.getBody();

                if (errorBody != null && errorBody.contains("invalid_grant")) {
                    throw new RuntimeException("인증 코드가 유효하지 않습니다. 새로운 인증 코드를 사용해주세요.");
                } else if (errorBody != null && errorBody.contains("KOE320")) {
                    throw new RuntimeException("인증 코드를 찾을 수 없습니다. 새로운 인증 코드를 사용해주세요.");
                } else {
                    throw new RuntimeException("카카오 토큰 요청 실패: " + response.getStatusCode() + " - " + errorBody);
                }
            }

            KakaoOAuthToken oAuthToken = objectMapper.readValue(response.getBody(), KakaoOAuthToken.class);
            
            if (oAuthToken.getAccess_token() == null || oAuthToken.getAccess_token().trim().isEmpty()) {
                throw new RuntimeException("카카오 액세스 토큰이 비어있습니다.");
            }
            
            return oAuthToken;
            
        } catch (RestClientException e) {
            log.error("카카오 토큰 요청 중 네트워크 오류: {}", e.getMessage());
            throw new RuntimeException("카카오 서버 연결 실패: " + e.getMessage());
        } catch (JsonProcessingException e) {
            log.error("카카오 토큰 응답 파싱 오류: {}", e.getMessage());
            throw new RuntimeException("카카오 토큰 응답 파싱 실패: " + e.getMessage());
        } catch (Exception e) {
            log.error("카카오 토큰 요청 중 예상치 못한 오류: {}", e.getMessage());
            throw new RuntimeException("카카오 토큰 요청 실패: " + e.getMessage());
        }
    }

    /**
     * 카카오 사용자 프로필 요청
     */
    public KakaoProfile requestProfile(KakaoOAuthToken oAuthToken) {
        if (oAuthToken == null || oAuthToken.getAccess_token() == null) {
            throw new IllegalArgumentException("유효하지 않은 OAuth 토큰입니다.");
        }
        
        log.info("카카오 프로필 요청 시작");
        
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + oAuthToken.getAccess_token());

        HttpEntity<MultiValueMap<String, String>> kakaoProfileRequest = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    "https://kapi.kakao.com/v2/user/me", 
                    HttpMethod.GET, 
                    kakaoProfileRequest, 
                    String.class
            );
            
            log.info("카카오 프로필 응답 상태: {}", response.getStatusCode());
            
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("카카오 프로필 요청 실패: " + response.getStatusCode() + " - " + response.getBody());
            }

            KakaoProfile kakaoProfile = objectMapper.readValue(response.getBody(), KakaoProfile.class);
            
            if (kakaoProfile.getId() == null) {
                throw new RuntimeException("카카오 사용자 ID가 비어있습니다.");
            }
            
            log.info("카카오 프로필 조회 완료: id={}", kakaoProfile.getId());
            return kakaoProfile;
            
        } catch (RestClientException e) {
            log.error("카카오 프로필 요청 중 네트워크 오류: {}", e.getMessage());
            throw new RuntimeException("카카오 서버 연결 실패: " + e.getMessage());
        } catch (JsonProcessingException e) {
            log.error("카카오 프로필 응답 파싱 오류: {}", e.getMessage());
            throw new RuntimeException("카카오 프로필 응답 파싱 실패: " + e.getMessage());
        } catch (Exception e) {
            log.error("카카오 프로필 요청 중 예상치 못한 오류: {}", e.getMessage());
            throw new RuntimeException("카카오 프로필 요청 실패: " + e.getMessage());
        }
    }
}
