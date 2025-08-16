package com.carecode.domain.app.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 애플리케이션 기본 API 컨트롤러
 * 루트 경로 및 기본 정보 제공
 */
@Slf4j
@RestController
@Tag(name = "애플리케이션", description = "애플리케이션 기본 정보 API")
public class AppController {

    /**
     * 애플리케이션 기본 정보
     */
    @GetMapping("/")
    @Operation(summary = "애플리케이션 정보", description = "CareCode API 서버의 기본 정보를 반환합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "정보 조회 성공")
    })
    public ResponseEntity<Map<String, Object>> getAppInfo() {
        log.info("애플리케이션 기본 정보 요청");
        
        Map<String, Object> appInfo = Map.of(
            "name", "CareCode API Server",
            "description", "육아 지원 플랫폼 CareCode의 REST API 서버",
            "version", "1.0.0",
            "status", "running",
            "timestamp", LocalDateTime.now(),
            "features", Map.of(
                "authentication", "JWT + OAuth2 (Kakao)",
                "database", "MariaDB",
                "documentation", "/swagger-ui.html"
            ),
            "endpoints", Map.of(
                "swagger", "/swagger-ui.html",
                "api_docs", "/api-docs",
                "kakao_login", "/oauth2/authorization/kakao",
                "profile_test", "/profile-test.html"
            )
        );
        
        return ResponseEntity.ok(appInfo);
    }

    /**
     * 헬스 체크
     */
    @GetMapping("/health")
    @Operation(summary = "헬스 체크", description = "서버 상태를 확인합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "서버 정상")
    })
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = Map.of(
            "status", "UP",
            "timestamp", LocalDateTime.now(),
            "uptime", "Running"
        );
        
        return ResponseEntity.ok(health);
    }
}