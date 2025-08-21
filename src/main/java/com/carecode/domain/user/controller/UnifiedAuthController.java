package com.carecode.domain.user.controller;

import com.carecode.core.annotation.LogExecutionTime;
import com.carecode.core.controller.BaseController;
import com.carecode.core.exception.UserNotFoundException;
import com.carecode.domain.user.dto.LoginRequestDto;
import com.carecode.domain.user.dto.RefreshTokenRequest;
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
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 통합 인증 컨트롤러
 * 일반 로그인, 카카오 로그인, 토큰 갱신, 회원가입 등 모든 인증 관련 API
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", allowedHeaders = "*")
@Tag(name = "인증", description = "통합 인증 API (일반 로그인, 토큰 갱신)")
public class UnifiedAuthController extends BaseController {

    private final UserService userService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    // ==================== 일반 로그인 ====================

    /**
     * 일반 로그인
     */
    @PostMapping("/login")
    @LogExecutionTime
    @Operation(summary = "일반 로그인", description = "이메일과 비밀번호로 로그인합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "로그인 성공"),
        @ApiResponse(responseCode = "401", description = "로그인 실패"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    public ResponseEntity<TokenDto> login(
            @Parameter(description = "로그인 정보", required = true) 
            @Valid @RequestBody LoginRequestDto request) {
        log.info("일반 로그인 요청: 이메일={}", request.getEmail());
        
        try {
            User userEntity = userService.getUserEntityByEmail(request.getEmail());
            
            // 비밀번호 검증
            if (!passwordEncoder.matches(request.getPassword(), userEntity.getPassword())) {
                log.warn("로그인 실패: 비밀번호 불일치 - 이메일={}", request.getEmail());
                return ResponseEntity.status(401).body(TokenDto.builder()
                        .success(false)
                        .message("비밀번호가 일치하지 않습니다.")
                        .build());
            }
            
            // 사용자 활성 상태 확인
            if (!userEntity.getIsActive()) {
                log.warn("로그인 실패: 비활성 사용자 - 이메일={}", request.getEmail());
                return ResponseEntity.status(401).body(TokenDto.builder()
                        .success(false)
                        .message("비활성화된 사용자입니다.")
                        .build());
            }
            
            // 마지막 로그인 시간 업데이트
            userEntity.setLastLoginAt(LocalDateTime.now());
            userEntity.setUpdatedAt(LocalDateTime.now());
            userService.saveUser(userEntity);
            
            // JWT 토큰 생성
            String accessToken = jwtService.generateAccessToken(
                userEntity.getUserId(), 
                userEntity.getEmail(), 
                userEntity.getRole().name(),
                userEntity.getName() // name 포함
            );
            
            String refreshToken = jwtService.generateRefreshToken(
                userEntity.getUserId(), 
                userEntity.getEmail()
            );
            
            TokenDto tokenDto = TokenDto.builder()
                    .success(true)
                    .message("로그인 성공!")
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(3600000L)
                    .refreshExpiresIn(2592000000L)
                    .user(userService.convertToDto(userEntity))
                    .build();
            
            return ResponseEntity.ok(tokenDto);
            
        } catch (UserNotFoundException e) {
            log.warn("로그인 실패: 사용자를 찾을 수 없음 - 이메일={}", request.getEmail());
            return ResponseEntity.status(401).body(TokenDto.builder()
                    .success(false)
                    .message("이메일 또는 비밀번호가 올바르지 않습니다.")
                    .build());
        } catch (Exception e) {
            log.error("로그인 처리 중 오류: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(TokenDto.builder()
                    .success(false)
                    .message("로그인 처리 중 오류가 발생했습니다.")
                    .build());
        }
    }

    /**
     * 회원가입
     */
    @PostMapping("/register")
    @LogExecutionTime
    @Operation(summary = "회원가입", description = "새로운 사용자를 등록합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "회원가입 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "409", description = "이미 존재하는 이메일")
    })
    public ResponseEntity<TokenDto> register(
            @Parameter(description = "회원가입 정보", required = true) 
            @Valid @RequestBody UserDto request) {
        log.info("회원가입 요청: 이메일={}", request.getEmail());
        
        try {
            UserDto createdUser = userService.createUser(request);
            
            // JWT 토큰 생성
            String accessToken = jwtService.generateAccessToken(
                createdUser.getUserId(), 
                createdUser.getEmail(), 
                createdUser.getRole(),
                createdUser.getName() // name 포함
            );
            
            String refreshToken = jwtService.generateRefreshToken(
                createdUser.getUserId(), 
                createdUser.getEmail()
            );
            
            TokenDto tokenDto = TokenDto.builder()
                    .success(true)
                    .message("회원가입 성공!")
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(3600000L)
                    .refreshExpiresIn(2592000000L)
                    .user(createdUser)
                    .build();
            
            return ResponseEntity.ok(tokenDto);
            
        } catch (IllegalArgumentException e) {
            log.warn("회원가입 실패: {}", e.getMessage());
            return ResponseEntity.status(400).body(TokenDto.builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        } catch (Exception e) {
            log.error("회원가입 처리 중 오류: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(TokenDto.builder()
                    .success(false)
                    .message("회원가입 처리 중 오류가 발생했습니다.")
                    .build());
        }
    }



    // ==================== 토큰 관리 ====================

    /**
     * 토큰 갱신
     */
    @PostMapping("/refresh")
    @LogExecutionTime
    @Operation(summary = "토큰 갱신", description = "Refresh Token을 사용하여 새로운 Access Token을 발급합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "토큰 갱신 성공"),
        @ApiResponse(responseCode = "401", description = "토큰 갱신 실패"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    public ResponseEntity<TokenDto> refreshToken(
            @Parameter(description = "토큰 갱신 정보", required = true) 
            @Valid @RequestBody RefreshTokenRequest request) {
        log.info("토큰 갱신 요청");
        
        try {
            // Refresh Token 검증
            if (!jwtService.validateToken(request.getRefreshToken())) {
                log.warn("토큰 갱신 실패: 유효하지 않은 Refresh Token");
                return ResponseEntity.status(401).body(TokenDto.builder()
                        .success(false)
                        .message("유효하지 않은 Refresh Token입니다.")
                        .build());
            }
            
            // Refresh Token에서 사용자 정보 추출
            String userId = jwtService.getUserIdFromToken(request.getRefreshToken());
            String email = jwtService.getEmailFromToken(request.getRefreshToken());
            
            // 사용자 존재 확인
            UserDto user = userService.getUserById(userId);
            
            // 새로운 Access Token 생성
            String newAccessToken = jwtService.generateAccessToken(userId, email, user.getRole(), user.getName());
            
            // 새로운 Refresh Token 생성 (토큰 로테이션)
            String newRefreshToken = jwtService.generateRefreshToken(userId, email);
            
            TokenDto tokenDto = TokenDto.builder()
                    .success(true)
                    .message("토큰 갱신 성공!")
                    .accessToken(newAccessToken)
                    .refreshToken(newRefreshToken)
                    .tokenType("Bearer")
                    .expiresIn(3600000L)
                    .refreshExpiresIn(2592000000L)
                    .user(user)
                    .build();
            
            return ResponseEntity.ok(tokenDto);
            
        } catch (Exception e) {
            log.error("토큰 갱신 처리 중 오류: {}", e.getMessage(), e);
            return ResponseEntity.status(401).body(TokenDto.builder()
                    .success(false)
                    .message("토큰 갱신에 실패했습니다.")
                    .build());
        }
    }

    // ==================== 유틸리티 메서드 ====================

    /**
     * 현재 로그인한 사용자의 이메일을 JWT 토큰에서 추출
     */
    private String getCurrentUserEmail(HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new IllegalArgumentException("유효한 Authorization 헤더가 없습니다.");
            }
            
            String token = authHeader.substring(7);
            return jwtService.extractEmailFromToken(token);
        } catch (Exception e) {
            log.error("JWT 토큰에서 이메일 추출 실패: {}", e.getMessage());
            throw new IllegalArgumentException("인증 토큰이 유효하지 않습니다.");
        }
    }
}
