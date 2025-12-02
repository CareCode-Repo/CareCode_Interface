package com.carecode.domain.user.controller;

import com.carecode.core.annotation.LogExecutionTime;
import com.carecode.core.controller.BaseController;
import com.carecode.core.exception.UserNotFoundException;
import com.carecode.domain.user.dto.request.KakaoRegistrationRequest;
import com.carecode.domain.user.dto.response.UserDto;
import com.carecode.domain.user.entity.User;
import com.carecode.domain.user.service.AuthService;
import com.carecode.domain.user.service.JwtService;
import com.carecode.domain.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 카카오 로그인 관련 통합 컨트롤러
 * 카카오 OAuth 로그인, 회원가입 완료, 토큰 갱신 등을 처리
 */
@RestController
@RequestMapping("/auth/kakao")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", allowedHeaders = "*")
@Tag(name = "카카오 인증", description = "카카오 OAuth 로그인 및 회원가입 API")
public class KakaoAuthController extends BaseController {

    private final AuthService authService;
    private final UserService userService;
    private final JwtService jwtService;

    /**
     * 카카오 OAuth 로그인/회원가입
     */
    @PostMapping("/login")
    @LogExecutionTime
    @Operation(summary = "카카오 OAuth 로그인/회원가입", description = "카카오 인증 코드를 받아 로그인하거나 신규 사용자를 생성합니다.")
    public ResponseEntity<Map<String, Object>> kakaoLogin(
            @Parameter(description = "카카오 인증 코드", required = true) @RequestParam String code,
            HttpServletResponse response) {
        try {
            log.info("카카오 OAuth 로그인 요청: code={}", code);
            
            User user = authService.oAuthLoginOrRegister(code, response);
            
            // Authorization 헤더에서 Access Token 추출
            String accessToken = response.getHeader("Authorization");
            if (accessToken != null && accessToken.startsWith("Bearer ")) {
                accessToken = accessToken.substring(7);
            }
            
            // Refresh Token 추출
            String refreshToken = response.getHeader("X-Refresh-Token");
            
            // 신규 사용자 여부 확인
            String isNewUser = response.getHeader("X-New-User");
            boolean isNew = "true".equals(isNewUser);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", isNew ? "카카오 회원가입 성공!" : "카카오 로그인 성공!",
                "isNewUser", isNew,
                "accessToken", accessToken,
                "refreshToken", refreshToken,
                "tokenType", "Bearer",
                "expiresIn", 3600000L,
                "refreshExpiresIn", 2592000000L, // 30일
                "user", Map.of(
                    "userId", user.getUserId(),
                    "email", user.getEmail(),
                    "name", user.getName(),
                    "role", user.getRole().name(),
                    "provider", user.getProvider(),
                    "emailVerified", user.getEmailVerified(),
                    "registrationCompleted", user.getRegistrationCompleted()
                )
            ));
            
        } catch (Exception e) {
            log.error("카카오 OAuth 처리 오류: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "카카오 OAuth 처리 실패: " + e.getMessage()
            ));
        }
    }

    /**
     * 카카오 신규 사용자 가입 완료 (이름 및 역할 설정)
     */
    @PostMapping("/complete-registration")
    @LogExecutionTime
    @Operation(summary = "카카오 신규 사용자 가입 완료", description = "카카오 로그인 후 신규 사용자의 이름과 역할을 설정하여 가입을 완료합니다.")
    public ResponseEntity<UserDto> completeRegistration(@Parameter(description = "가입 완료 정보", required = true) @Valid @RequestBody KakaoRegistrationRequest request, HttpServletRequest httpRequest) {
        try {
            // JWT 토큰에서 이메일을 추출
            String email = getCurrentUserEmail(httpRequest);
            
            UserDto updatedUser = userService.completeKakaoRegistration(email, request.getName(), request.getRole());
            log.info("카카오 사용자 가입 완료 성공: email={}, name={}, role={}", email, request.getName(), request.getRole());
            return ResponseEntity.ok(updatedUser);
        } catch (UserNotFoundException e) {
            log.error("카카오 사용자 가입 완료 실패 - 사용자를 찾을 수 없음: error={}", e.getMessage());
            throw e;
        } catch (IllegalArgumentException e) {
            log.error("카카오 사용자 가입 완료 실패 - 잘못된 요청: name={}, role={}, error={}", request.getName(), request.getRole(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("카카오 사용자 가입 완료 실패 - 예상치 못한 오류: error={}", e.getMessage(), e);
            throw new RuntimeException("카카오 사용자 가입 완료 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 카카오 로그인 URL 생성
     */
    @GetMapping("/login-url")
    @LogExecutionTime
    @Operation(summary = "카카오 로그인 URL 생성", description = "카카오 OAuth 로그인을 위한 URL을 생성합니다.")
    public ResponseEntity<Map<String, Object>> getKakaoLoginUrl() {
        try {
            log.info("카카오 로그인 URL 생성 요청");
            
            // 카카오 OAuth URL 생성
            String clientId = "your_kakao_client_id"; // application.properties에서 가져와야 함
            String redirectUri = "http://localhost:3000/auth/kakao/callback";
            String kakaoLoginUrl = String.format(
                "https://kauth.kakao.com/oauth/authorize?client_id=%s&redirect_uri=%s&response_type=code",
                clientId, redirectUri
            );
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "loginUrl", kakaoLoginUrl,
                "message", "카카오 로그인 URL이 생성되었습니다."
            ));
            
        } catch (Exception e) {
            log.error("카카오 로그인 URL 생성 오류: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "카카오 로그인 URL 생성 실패: " + e.getMessage()
            ));
        }
    }

    /**
     * 현재 로그인한 사용자의 이메일을 JWT 토큰에서 추출
     */
    private String getCurrentUserEmail(HttpServletRequest request) {
        // Authorization 헤더에서 토큰 추출
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("유효한 Authorization 헤더가 없습니다.");
        }

        String token = authHeader.substring(7); // "Bearer " 제거

        // JWT 토큰에서 이메일 추출
        return jwtService.extractEmailFromToken(token);
    }
}
