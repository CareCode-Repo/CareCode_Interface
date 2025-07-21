package com.carecode.domain.user.controller;

import com.carecode.core.annotation.LogExecutionTime;
import com.carecode.core.controller.BaseController;
import com.carecode.core.exception.CareServiceException;
import com.carecode.core.exception.UserNotFoundException;
import com.carecode.domain.user.dto.LoginRequestDto;
import com.carecode.domain.user.dto.TokenDto;
import com.carecode.domain.user.dto.UserDto;
import com.carecode.domain.user.entity.User;
import com.carecode.domain.user.service.JwtService;
import com.carecode.domain.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 인증 API 컨트롤러
 * JWT 토큰 기반 인증 서비스
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "인증", description = "JWT 토큰 기반 인증 API")
public class AuthController extends BaseController {

    private final UserService userService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    /**
     * 로그인
     */
    @PostMapping("/login")
    @LogExecutionTime
    @Operation(summary = "로그인", description = "사용자 로그인 후 JWT 토큰을 발급합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "로그인 성공",
            content = @Content(schema = @Schema(implementation = TokenDto.class))),
        @ApiResponse(responseCode = "401", description = "로그인 실패",
            content = @Content(schema = @Schema(implementation = TokenDto.class))),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<TokenDto> login(
            @Parameter(description = "로그인 정보", required = true) @RequestBody LoginRequestDto request) {
        log.info("로그인 요청: 이메일={}", request.getEmail());
        
        try {
            // 사용자 엔티티 조회 (비밀번호 포함)
            User userEntity = userService.getUserEntityByEmail(request.getEmail());
            
            // 비밀번호 검증
            if (!passwordEncoder.matches(request.getPassword(), userEntity.getPassword())) {
                log.warn("로그인 실패: 비밀번호 불일치 - 이메일={}", request.getEmail());
                TokenDto errorResponse = TokenDto.builder()
                        .success(false)
                        .message("비밀번호가 일치하지 않습니다.")
                        .build();
                return ResponseEntity.status(401).body(errorResponse);
            }
            
            // 사용자 활성 상태 확인
            if (!userEntity.getIsActive()) {
                log.warn("로그인 실패: 비활성 사용자 - 이메일={}", request.getEmail());
                TokenDto errorResponse = TokenDto.builder()
                        .success(false)
                        .message("비활성화된 사용자입니다.")
                        .build();
                return ResponseEntity.status(401).body(errorResponse);
            }
            
            // 마지막 로그인 시간 업데이트
            userEntity.setLastLoginAt(LocalDateTime.now());
            userEntity.setUpdatedAt(LocalDateTime.now());
            
            // JWT 토큰 생성
            String accessToken = jwtService.generateAccessToken(
                userEntity.getUserId(), 
                userEntity.getEmail(), 
                userEntity.getRole().name()
            );
            String refreshToken = jwtService.generateRefreshToken(
                userEntity.getUserId(), 
                userEntity.getEmail()
            );
            
            TokenDto tokenResponse = TokenDto.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(3600000L) // 1시간
                    .userId(userEntity.getUserId())
                    .email(userEntity.getEmail())
                    .role(userEntity.getRole().name())
                    .success(true)
                    .message("로그인 성공")
                    .build();
            
            log.info("로그인 성공: 이메일={}", request.getEmail());
            return ResponseEntity.ok(tokenResponse);
            
        } catch (UserNotFoundException e) {
            log.warn("로그인 실패: 사용자를 찾을 수 없음 - 이메일={}", request.getEmail());
            TokenDto errorResponse = TokenDto.builder()
                    .success(false)
                    .message("사용자를 찾을 수 없습니다.")
                    .build();
            return ResponseEntity.status(401).body(errorResponse);
        } catch (Exception e) {
            log.error("로그인 처리 오류: {}", e.getMessage());
            throw new CareServiceException("로그인 처리 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 회원가입
     */
    @PostMapping("/register")
    @LogExecutionTime
    @Operation(summary = "회원가입", description = "새로운 사용자를 등록합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "회원가입 성공",
            content = @Content(schema = @Schema(implementation = UserDto.class))),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "409", description = "이미 존재하는 사용자"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<UserDto> register(
            @Parameter(description = "회원가입 정보", required = true) @RequestBody UserDto userDto) {
        log.info("회원가입 요청: 이메일={}", userDto.getEmail());
        
        try {
            // 비밀번호 암호화
            userDto.setPassword(passwordEncoder.encode(userDto.getPassword()));
            
            // 기본 역할 설정
            if (userDto.getRole() == null) {
                userDto.setRole("PARENT");
            }
            
            // 활성 상태 설정
            userDto.setIsActive(true);
            userDto.setEmailVerified(false);
            
            UserDto createdUser = userService.createUser(userDto);
            
            // 비밀번호는 응답에서 제거
            createdUser.setPassword(null);
            
            log.info("회원가입 성공: 이메일={}", userDto.getEmail());
            return ResponseEntity.ok(createdUser);
            
        } catch (Exception e) {
            log.error("회원가입 처리 오류: {}", e.getMessage());
            throw new CareServiceException("회원가입 처리 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 토큰 갱신
     */
    @PostMapping("/refresh")
    @LogExecutionTime
    @Operation(summary = "토큰 갱신", description = "Refresh Token을 사용하여 새로운 Access Token을 발급합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "토큰 갱신 성공",
            content = @Content(schema = @Schema(implementation = TokenDto.class))),
        @ApiResponse(responseCode = "401", description = "유효하지 않은 Refresh Token"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<TokenDto> refreshToken(
            @Parameter(description = "토큰 갱신 요청", required = true) @RequestBody TokenDto.RefreshTokenRequest request) {
        log.info("토큰 갱신 요청");
        
        try {
            TokenDto newTokens = jwtService.refreshTokens(request.getRefreshToken());
            log.info("토큰 갱신 성공: userId={}", newTokens.getUserId());
            return ResponseEntity.ok(newTokens);
            
        } catch (Exception e) {
            log.error("토큰 갱신 실패: {}", e.getMessage());
            return ResponseEntity.status(401).build();
        }
    }

    /**
     * 토큰 검증
     */
    @PostMapping("/validate")
    @LogExecutionTime
    @Operation(summary = "토큰 검증", description = "Access Token의 유효성을 검증합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "토큰 검증 성공",
            content = @Content(schema = @Schema(implementation = TokenDto.TokenValidationResponse.class))),
        @ApiResponse(responseCode = "401", description = "유효하지 않은 토큰"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<TokenDto.TokenValidationResponse> validateToken(
            @Parameter(description = "토큰 검증 요청", required = true) @RequestBody TokenDto.TokenValidationRequest request) {
        log.info("토큰 검증 요청");
        
        try {
            TokenDto.TokenValidationResponse response = jwtService.validateTokenAndExtractInfo(request.getAccessToken());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("토큰 검증 실패: {}", e.getMessage());
            return ResponseEntity.status(401).build();
        }
    }

    /**
     * 로그아웃
     */
    @PostMapping("/logout")
    @LogExecutionTime
    @Operation(summary = "로그아웃", description = "사용자 로그아웃을 처리합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "로그아웃 성공"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Map<String, String>> logout() {
        log.info("로그아웃 요청");
        
        // 클라이언트에서 토큰을 삭제하도록 안내
        // 서버에서는 별도의 토큰 블랙리스트 관리가 필요할 수 있음
        return ResponseEntity.ok(Map.of("message", "로그아웃되었습니다."));
    }

    /**
     * 현재 사용자 정보 조회
     */
    @GetMapping("/me")
    @LogExecutionTime
    @Operation(summary = "현재 사용자 정보 조회", description = "현재 인증된 사용자의 정보를 조회합니다.")
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공",
            content = @Content(schema = @Schema(implementation = UserDto.class))),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<UserDto> getCurrentUser() {
        try {
            // SecurityContext에서 현재 사용자 ID 가져오기
            String currentUserId = org.springframework.security.core.context.SecurityContextHolder
                    .getContext()
                    .getAuthentication()
                    .getName();
            
            log.info("현재 인증된 사용자 ID: {}", currentUserId);
            
            // anonymousUser인 경우 처리
            if ("anonymousUser".equals(currentUserId)) {
                log.warn("인증되지 않은 사용자 접근");
                return ResponseEntity.status(401).build();
            }
            
            UserDto user = userService.getUserById(currentUserId);
            user.setPassword(null); // 비밀번호는 응답에서 제거
            
            return ResponseEntity.ok(user);
            
        } catch (Exception e) {
            log.error("현재 사용자 정보 조회 실패: {}", e.getMessage());
            throw new CareServiceException("현재 사용자 정보 조회 중 오류가 발생했습니다.", e);
        }
    }
} 