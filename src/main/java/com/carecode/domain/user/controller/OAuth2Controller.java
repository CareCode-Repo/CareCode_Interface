package com.carecode.domain.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * OAuth2 소셜 로그인 컨트롤러
 * 카카오, 구글 등 소셜 로그인 관련 엔드포인트 제공
 */
@Slf4j
@RestController
@RequestMapping("/oauth2")
@RequiredArgsConstructor
@Tag(name = "소셜 로그인", description = "OAuth2 소셜 로그인 API")
public class OAuth2Controller {

    /**
     * 카카오 로그인 페이지로 리다이렉트
     */
    @GetMapping("/authorization/kakao")
    @Operation(summary = "카카오 로그인", description = "카카오 로그인 페이지로 리다이렉트합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "302", description = "카카오 로그인 페이지로 리다이렉트"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    public void kakaoLogin() {
        // Spring Security OAuth2가 자동으로 카카오 로그인 페이지로 리다이렉트
        // 실제 처리는 /oauth2/authorization/kakao 경로에서 이뤄짐
    }

    /**
     * 구글 로그인 페이지로 리다이렉트
     */
    @GetMapping("/authorization/google")
    @Operation(summary = "구글 로그인", description = "구글 로그인 페이지로 리다이렉트합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "302", description = "구글 로그인 페이지로 리다이렉트"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    public void googleLogin() {
        // Spring Security OAuth2가 자동으로 구글 로그인 페이지로 리다이렉트
        // 실제 처리는 /oauth2/authorization/google 경로에서 이뤄짐
    }

    /**
     * 소셜 로그인 성공 후 토큰 정보 확인
     */
    @GetMapping("/success")
    @Operation(summary = "소셜 로그인 성공 정보", description = "소셜 로그인 성공 후 토큰 정보를 확인합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "토큰 정보 반환"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    public ResponseEntity<Map<String, Object>> loginSuccess(
            @Parameter(description = "액세스 토큰") @RequestParam(required = false) String accessToken,
            @Parameter(description = "리프레시 토큰") @RequestParam(required = false) String refreshToken,
            @Parameter(description = "토큰 타입") @RequestParam(required = false) String tokenType,
            @Parameter(description = "만료 시간") @RequestParam(required = false) Long expiresIn,
            @Parameter(description = "사용자 ID") @RequestParam(required = false) String userId,
            @Parameter(description = "이메일") @RequestParam(required = false) String email,
            @Parameter(description = "역할") @RequestParam(required = false) String role,
            @Parameter(description = "성공 여부") @RequestParam(required = false) Boolean success
    ) {
        log.info("소셜 로그인 성공 정보 요청: 사용자={}, 성공={}", email, success);

        Map<String, Object> response = Map.of(
            "success", success != null ? success : false,
            "accessToken", accessToken != null ? accessToken : "",
            "refreshToken", refreshToken != null ? refreshToken : "",
            "tokenType", tokenType != null ? tokenType : "Bearer",
            "expiresIn", expiresIn != null ? expiresIn : 3600000L,
            "user", Map.of(
                "userId", userId != null ? userId : "",
                "email", email != null ? email : "",
                "role", role != null ? role : ""
            )
        );

        return ResponseEntity.ok(response);
    }

    /**
     * 소셜 로그인 실패 정보
     */
    @GetMapping("/failure")
    @Operation(summary = "소셜 로그인 실패 정보", description = "소셜 로그인 실패 정보를 확인합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "400", description = "로그인 실패"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Map<String, Object>> loginFailure(
            @Parameter(description = "오류 메시지") @RequestParam(required = false) String error,
            @Parameter(description = "오류 타입") @RequestParam(required = false) String errorType
    ) {
        log.warn("소셜 로그인 실패: 오류={}, 타입={}", error, errorType);

        Map<String, Object> response = Map.of(
            "success", false,
            "error", error != null ? error : "알 수 없는 오류가 발생했습니다.",
            "errorType", errorType != null ? errorType : "unknown_error"
        );

        return ResponseEntity.badRequest().body(response);
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
                "textColor", "#000000"
            ),
            "google", Map.of(
                "name", "구글",
                "loginUrl", "/oauth2/authorization/google", 
                "color", "#4285F4",
                "textColor", "#FFFFFF"
            )
        );

        return ResponseEntity.ok(Map.of(
            "success", true,
            "providers", providers
        ));
    }
}