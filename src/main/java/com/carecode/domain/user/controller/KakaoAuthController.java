package com.carecode.domain.user.controller;

import com.carecode.core.annotation.LogExecutionTime;
import com.carecode.core.controller.BaseController;
import com.carecode.core.exception.UserNotFoundException;
import com.carecode.core.security.CurrentUserFacade;
import com.carecode.core.util.KakaoUtil;
import com.carecode.domain.user.dto.request.KakaoRegistrationRequest;
import com.carecode.domain.user.dto.response.TokenDto;
import com.carecode.domain.user.dto.response.UserDto;
import com.carecode.domain.user.service.AuthService;
import com.carecode.domain.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "카카오 인증", description = "카카오 OAuth 로그인 및 회원가입 API")
public class KakaoAuthController extends BaseController {

    private final AuthService authService;
    private final UserService userService;
    private final KakaoUtil kakaoUtil;
    private final CurrentUserFacade currentUserFacade;

    @PostMapping("/login")
    @LogExecutionTime
    @Operation(summary = "카카오 OAuth 로그인/회원가입", description = "카카오 인증 코드를 받아 로그인하거나 신규 사용자를 생성합니다.")
    public ResponseEntity<TokenDto> kakaoLogin(
            @Parameter(description = "카카오 인증 코드", required = true) @RequestParam String code) {
        log.info("카카오 OAuth 로그인 요청 수신 (authorization code는 로그에 기록하지 않음)");
        TokenDto body = authService.oAuthLoginOrRegister(code);
        return ResponseEntity.ok(body);
    }

    @PostMapping("/complete-registration")
    @LogExecutionTime
    @Operation(summary = "카카오 신규 사용자 가입 완료", description = "카카오 로그인 후 신규 사용자의 이름과 역할을 설정하여 가입을 완료합니다.")
    public ResponseEntity<UserDto> completeRegistration(
            @Parameter(description = "가입 완료 정보", required = true) @Valid @RequestBody KakaoRegistrationRequest request) {
        String email = currentUserFacade.requireCurrentUserEmail();
        UserDto updatedUser = userService.completeKakaoRegistration(email, request.getName(), request.getRole());
        log.info("카카오 사용자 가입 완료: email={}, role={}", email, request.getRole());
        return ResponseEntity.ok(updatedUser);
    }

    @GetMapping("/login-url")
    @LogExecutionTime
    @Operation(summary = "카카오 로그인 URL 생성", description = "카카오 OAuth 로그인을 위한 URL을 생성합니다.")
    public ResponseEntity<Map<String, Object>> getKakaoLoginUrl() {
        log.debug("카카오 로그인 URL 생성 요청");
        String kakaoLoginUrl = kakaoUtil.buildAuthorizationUrl();
        return ResponseEntity.ok(Map.of(
                "success", true,
                "loginUrl", kakaoLoginUrl,
                "message", "카카오 로그인 URL이 생성되었습니다."
        ));
    }
}
