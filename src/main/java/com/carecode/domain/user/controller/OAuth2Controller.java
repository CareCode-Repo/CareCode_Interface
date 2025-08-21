package com.carecode.domain.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;

import java.util.Map;

/**
 * OAuth2 소셜 로그인 컨트롤러
 * 카카오 로그인 관련 엔드포인트 제공
 */
@Slf4j
@RestController
@RequestMapping("/oauth2")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", allowedHeaders = "*") // CORS 허용
@Tag(name = "소셜 로그인", description = "OAuth2 소셜 로그인 API")
public class OAuth2Controller {

    @Value("${server.port:8081}")
    private String serverPort;
    
    @Value("${server.address:13.209.36.209}")
    private String serverAddress;
    
    @Value("${spring.security.oauth2.client.registration.kakao.client-id}")
    private String kakaoClientId;

    @Value("${kakao.redirect-uri}")
    private String kakaoRedirectUri;

    /**
     * 카카오 로그인 페이지로 리다이렉트
     */
    @GetMapping("/authorization/kakao")
    @Operation(summary = "카카오 로그인", description = "카카오 로그인 페이지로 리다이렉트합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "302", description = "카카오 로그인 페이지로 리다이렉트"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    public ResponseEntity<Map<String, Object>> kakaoLogin() {
        log.info("카카오 로그인 페이지 요청");
        
        String kakaoAuthUrl = String.format(
            "https://kauth.kakao.com/oauth/authorize?client_id=%s&redirect_uri=%s&response_type=code",
            kakaoClientId, kakaoRedirectUri
        );
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "카카오 로그인 페이지로 이동하세요",
            "authUrl", kakaoAuthUrl
        ));
    }

    /**
     * 소셜 로그인 제공자 목록
     */
    @GetMapping("/providers")
    @Operation(summary = "소셜 로그인 제공자 목록", description = "지원하는 소셜 로그인 제공자 목록을 반환합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "제공자 목록 반환")
    })
    public ResponseEntity<Map<String, Object>> getProviders() {
        Map<String, Object> providers = Map.of(
            "kakao", Map.of(
                "name", "카카오",
                "loginUrl", "/oauth2/authorization/kakao",
                "color", "#FEE500",
                "textColor", "#000000",
                "enabled", true
            )
        );

        return ResponseEntity.ok(Map.of(
            "success", true,
            "providers", providers
        ));
    }

    /**
     * OAuth2 설정 상태 확인
     */
    @GetMapping("/config/status")
    @Operation(summary = "OAuth2 설정 상태", description = "OAuth2 설정 상태를 확인합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "설정 상태 반환")
    })
    public ResponseEntity<Map<String, Object>> getOAuth2ConfigStatus() {
        Map<String, Object> status = Map.of(
            "kakao", Map.of(
                "enabled", true,
                "clientId", kakaoClientId,
                "redirectUri", kakaoRedirectUri,
                "scope", "profile_nickname,profile_image",
                "authorizationUri", "https://kauth.kakao.com/oauth/authorize",
                "tokenUri", "https://kauth.kakao.com/oauth/token",
                "userInfoUri", "https://kapi.kakao.com/v2/user/me"
            )
        );

        return ResponseEntity.ok(Map.of(
            "success", true,
            "config", status,
            "message", "OAuth2 configuration status"
        ));
    }

    /**
     * 프론트엔드용 카카오 인증 URL 제공
     */
    @GetMapping("/kakao/auth-url")
    @Operation(summary = "카카오 인증 URL", description = "프론트엔드에서 사용할 카카오 인증 URL을 제공합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "인증 URL 반환")
    })
    public ResponseEntity<Map<String, Object>> getKakaoAuthUrl() {
        log.info("카카오 인증 URL 요청");
        
        String kakaoAuthUrl = String.format(
            "https://kauth.kakao.com/oauth/authorize?client_id=%s&redirect_uri=%s&response_type=code",
            kakaoClientId, kakaoRedirectUri
        );
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "authUrl", kakaoAuthUrl,
            "clientId", kakaoClientId,
            "redirectUri", kakaoRedirectUri
        ));
    }
}