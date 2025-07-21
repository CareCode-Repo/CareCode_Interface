package com.carecode.domain.user.controller;

import com.carecode.core.annotation.LogExecutionTime;
import com.carecode.core.annotation.RequireAuthentication;
import com.carecode.core.controller.BaseController;
import com.carecode.core.exception.CareServiceException;
import com.carecode.core.exception.UserNotFoundException;
import com.carecode.domain.user.dto.UserDto;
import com.carecode.domain.user.dto.LoginRequestDto;
import com.carecode.domain.user.dto.PasswordChangeRequestDto;
import com.carecode.domain.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 사용자 API 컨트롤러
 * 사용자 프로필 및 계정 관리 서비스
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "사용자 관리", description = "사용자 프로필 및 계정 관리 API")
public class UserController extends BaseController {
    
    private final UserService userService;
    private static final String SUCCESS_MESSAGE = "처리가 완료되었습니다.";
    
    /**
     * 사용자 프로필 조회
     */
    @GetMapping("/{userId}")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "사용자 프로필 조회", description = "특정 사용자의 프로필 정보를 조회합니다.")
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공",
            content = @Content(schema = @Schema(implementation = UserDto.class))),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<UserDto> getUserProfile(
            @Parameter(description = "사용자 ID", required = true) @PathVariable String userId) {
        log.info("사용자 프로필 조회: 사용자ID={}", userId);
        
        try {
            UserDto user = userService.getUserById(userId);
            return ResponseEntity.ok(user);
        } catch (UserNotFoundException e) {
            log.error("사용자를 찾을 수 없음: {}", e.getMessage());
            throw e;
        } catch (IllegalArgumentException e) {
            log.error("잘못된 요청: {}", e.getMessage());
            throw e;
        }
    }
    
    /**
     * 사용자 프로필 업데이트
     */
    @PutMapping("/{userId}")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "사용자 프로필 업데이트", description = "사용자의 프로필 정보를 업데이트합니다.")
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "업데이트 성공",
            content = @Content(schema = @Schema(implementation = UserDto.class))),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<UserDto> updateUserProfile(
            @Parameter(description = "사용자 ID", required = true) @PathVariable String userId,
            @Parameter(description = "업데이트할 사용자 정보", required = true) @RequestBody UserDto userDto) {
        log.info("사용자 프로필 업데이트: 사용자ID={}", userId);
        
        try {
            UserDto updatedUser = userService.updateUser(userId, userDto);
            return ResponseEntity.ok(updatedUser);
        } catch (UserNotFoundException e) {
            log.error("사용자를 찾을 수 없음: {}", e.getMessage());
            throw e;
        } catch (IllegalArgumentException e) {
            log.error("잘못된 요청: {}", e.getMessage());
            throw e;
        }
    }
    
    /**
     * 사용자 등록
     */
    @PostMapping
    @LogExecutionTime
    @Operation(summary = "사용자 등록", description = "새로운 사용자를 등록합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "등록 성공",
            content = @Content(schema = @Schema(implementation = UserDto.class))),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "409", description = "이미 존재하는 사용자"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<UserDto> createUser(
            @Parameter(description = "등록할 사용자 정보", required = true) @RequestBody UserDto userDto) {
        log.info("사용자 등록: 이메일={}", userDto.getEmail());
        
        try {
            UserDto createdUser = userService.createUser(userDto);
            return ResponseEntity.ok(createdUser);
        } catch (IllegalArgumentException e) {
            log.error("사용자 등록 오류: {}", e.getMessage());
            throw e;
        }
    }
    
    /**
     * 사용자 삭제
     */
    @DeleteMapping("/{userId}")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "사용자 삭제", description = "특정 사용자를 삭제합니다.")
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "삭제 성공"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Map<String, String>> deleteUser(
            @Parameter(description = "사용자 ID", required = true) @PathVariable String userId) {
        log.info("사용자 삭제: 사용자ID={}", userId);
        
        try {
            userService.deleteUser(userId);
            return ResponseEntity.ok(Map.of("message", SUCCESS_MESSAGE));
        } catch (UserNotFoundException e) {
            log.error("사용자를 찾을 수 없음: {}", e.getMessage());
            throw e;
        } catch (IllegalArgumentException e) {
            log.error("잘못된 요청: {}", e.getMessage());
            throw e;
        }
    }
    
    /**
     * 사용자 목록 조회
     */
    @GetMapping
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "사용자 목록 조회", description = "모든 사용자 목록을 조회합니다.")
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공",
            content = @Content(schema = @Schema(implementation = UserDto.class))),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<List<UserDto>> getAllUsers() {
        log.info("사용자 목록 조회");
        
        try {
            List<UserDto> users = userService.getAllUsers();
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            log.error("사용자 목록 조회 오류: {}", e.getMessage());
            throw e;
        }
    }
    
    /**
     * 사용자 검색
     */
    @GetMapping("/search")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "사용자 검색", description = "키워드로 사용자를 검색합니다.")
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "검색 성공",
            content = @Content(schema = @Schema(implementation = UserDto.class))),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<List<UserDto>> searchUsers(
            @Parameter(description = "검색 키워드", required = true) @RequestParam String keyword) {
        log.info("사용자 검색: 키워드={}", keyword);
        
        try {
            List<UserDto> users = userService.searchUsers(keyword);
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            log.error("사용자 검색 오류: {}", e.getMessage());
            throw e;
        }
    }
    
    /**
     * 이메일로 사용자 조회
     */
    @GetMapping("/email/{email}")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "이메일로 사용자 조회", description = "이메일로 특정 사용자를 조회합니다.")
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공",
            content = @Content(schema = @Schema(implementation = UserDto.class))),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<UserDto> getUserByEmail(
            @Parameter(description = "이메일", required = true) @PathVariable String email) {
        log.info("사용자 조회 요청 - 이메일: {}", email);
        
        try {
            UserDto user = userService.getUserByEmail(email);
            return ResponseEntity.ok(user);
        } catch (UserNotFoundException e) {
            log.error("사용자를 찾을 수 없음: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("사용자 조회 오류: {}", e.getMessage());
            throw e;
        }
    }
    
    /**
     * 활성 사용자 조회
     */
    @GetMapping("/active")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "활성 사용자 조회", description = "활성 상태인 사용자 목록을 조회합니다.")
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공",
            content = @Content(schema = @Schema(implementation = UserDto.class))),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<List<UserDto>> getActiveUsers() {
        log.info("활성 사용자 조회");
        
        try {
            List<UserDto> users = userService.getActiveUsers();
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            log.error("활성 사용자 조회 오류: {}", e.getMessage());
            throw e;
        }
    }
    
    /**
     * 사용자 유형별 조회
     */
    @GetMapping("/type/{userType}")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "사용자 유형별 조회", description = "특정 유형의 사용자 목록을 조회합니다.")
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공",
            content = @Content(schema = @Schema(implementation = UserDto.class))),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<List<UserDto>> getUsersByType(
            @Parameter(description = "사용자 유형", required = true) @PathVariable String userType) {
        log.info("사용자 유형별 조회: 유형={}", userType);
        
        try {
            List<UserDto> users = userService.getUsersByType(userType);
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            log.error("사용자 유형별 조회 오류: {}", e.getMessage());
            throw e;
        }
    }
    
    /**
     * 지역별 사용자 조회
     */
    @GetMapping("/region/{region}")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "지역별 사용자 조회", description = "특정 지역의 사용자 목록을 조회합니다.")
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공",
            content = @Content(schema = @Schema(implementation = UserDto.class))),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<List<UserDto>> getUsersByRegion(
            @Parameter(description = "지역", required = true) @PathVariable String region) {
        log.info("지역별 사용자 조회: 지역={}", region);
        
        try {
            List<UserDto> users = userService.getUsersByRegion(region);
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            log.error("지역별 사용자 조회 오류: {}", e.getMessage());
            throw e;
        }
    }
    
    /**
     * 인증된 사용자 조회
     */
    @GetMapping("/verified")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "인증된 사용자 조회", description = "인증이 완료된 사용자 목록을 조회합니다.")
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공",
            content = @Content(schema = @Schema(implementation = UserDto.class))),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<List<UserDto>> getVerifiedUsers() {
        log.info("인증된 사용자 조회");
        
        try {
            List<UserDto> users = userService.getVerifiedUsers();
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            log.error("인증된 사용자 조회 오류: {}", e.getMessage());
            throw e;
        }
    }
    
    /**
     * 최근 활동 사용자 조회
     */
    @GetMapping("/recent")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "최근 활동 사용자 조회", description = "최근에 활동한 사용자 목록을 조회합니다.")
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공",
            content = @Content(schema = @Schema(implementation = UserDto.class))),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<List<UserDto>> getRecentlyActiveUsers() {
        log.info("최근 활동 사용자 조회");
        
        try {
            List<UserDto> users = userService.getRecentlyActiveUsers();
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            log.error("최근 활동 사용자 조회 오류: {}", e.getMessage());
            throw e;
        }
    }
    
    /**
     * 사용자 로그인 (기존 메서드 - JWT 인증으로 대체됨)
     * @deprecated JWT 인증을 위해 /auth/login을 사용하세요
     */
    @PostMapping("/login")
    @LogExecutionTime
    @Operation(summary = "사용자 로그인 (Deprecated)", description = "이 메서드는 더 이상 사용되지 않습니다. /auth/login을 사용하세요.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "로그인 성공",
            content = @Content(schema = @Schema(implementation = UserDto.class))),
        @ApiResponse(responseCode = "401", description = "로그인 실패"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @Deprecated
    public ResponseEntity<UserDto> login(
            @Parameter(description = "로그인 정보", required = true) @RequestBody LoginRequestDto request) {
        log.warn("Deprecated 로그인 메서드 호출됨: 이메일={}", request.getEmail());
        
        try {
            UserDto user = userService.login(request);
            return ResponseEntity.ok(user);
        } catch (IllegalArgumentException e) {
            log.error("로그인 오류: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("로그인 처리 오류: {}", e.getMessage());
            throw e;
        }
    }
    
    /**
     * 비밀번호 변경
     */
    @PutMapping("/{userId}/password")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "비밀번호 변경", description = "사용자의 비밀번호를 변경합니다.")
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "비밀번호 변경 성공"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Void> changePassword(
            @Parameter(description = "사용자 ID", required = true) @PathVariable String userId,
            @Parameter(description = "비밀번호 변경 정보", required = true) @RequestBody PasswordChangeRequestDto request) {
        log.info("비밀번호 변경: 사용자ID={}", userId);
        
        try {
            userService.changePassword(userId, request);
            return ResponseEntity.ok().build();
        } catch (UserNotFoundException e) {
            log.error("사용자를 찾을 수 없음: {}", e.getMessage());
            throw e;
        } catch (IllegalArgumentException e) {
            log.error("잘못된 요청: {}", e.getMessage());
            throw e;
        }
    }
    
    /**
     * 사용자 비활성화
     */
    @PutMapping("/{userId}/deactivate")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "사용자 비활성화", description = "사용자 계정을 비활성화합니다.")
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "비활성화 성공"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Void> deactivateUser(
            @Parameter(description = "사용자 ID", required = true) @PathVariable String userId) {
        log.info("사용자 비활성화: 사용자ID={}", userId);
        
        try {
            userService.deactivateUser(userId);
            return ResponseEntity.ok().build();
        } catch (UserNotFoundException e) {
            log.error("사용자를 찾을 수 없음: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("사용자 비활성화 오류: {}", e.getMessage());
            throw e;
        }
    }
    
    /**
     * 사용자 활성화
     */
    @PutMapping("/{userId}/activate")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "사용자 활성화", description = "사용자 계정을 활성화합니다.")
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "활성화 성공"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Void> activateUser(
            @Parameter(description = "사용자 ID", required = true) @PathVariable String userId) {
        log.info("사용자 활성화: 사용자ID={}", userId);
        
        try {
            userService.activateUser(userId);
            return ResponseEntity.ok().build();
        } catch (UserNotFoundException e) {
            log.error("사용자를 찾을 수 없음: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("사용자 활성화 오류: {}", e.getMessage());
            throw e;
        }
    }
    
    /**
     * 사용자 인증
     */
    @PutMapping("/{userId}/verify")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "사용자 인증", description = "사용자 계정을 인증합니다.")
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "인증 성공"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Void> verifyUser(
            @Parameter(description = "사용자 ID", required = true) @PathVariable String userId) {
        log.info("사용자 인증: 사용자ID={}", userId);
        
        try {
            userService.verifyUser(userId);
            return ResponseEntity.ok().build();
        } catch (UserNotFoundException e) {
            log.error("사용자를 찾을 수 없음: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("사용자 인증 오류: {}", e.getMessage());
            throw e;
        }
    }
    
    /**
     * 사용자 위치 업데이트
     */
    @PutMapping("/{userId}/location")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "사용자 위치 업데이트", description = "사용자의 위치 정보를 업데이트합니다.")
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "위치 업데이트 성공"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음"),
        @ApiResponse(responseCode = "400", description = "잘못된 위치 정보"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Void> updateUserLocation(
            @Parameter(description = "사용자 ID", required = true) @PathVariable String userId,
            @Parameter(description = "위도", required = true) @RequestParam Double latitude,
            @Parameter(description = "경도", required = true) @RequestParam Double longitude) {
        log.info("사용자 위치 업데이트: 사용자ID={}, 위도={}, 경도={}", userId, latitude, longitude);
        
        try {
            userService.updateUserLocation(userId, latitude, longitude);
            return ResponseEntity.ok().build();
        } catch (UserNotFoundException e) {
            log.error("사용자를 찾을 수 없음: {}", e.getMessage());
            throw e;
        } catch (IllegalArgumentException e) {
            log.error("잘못된 위치 정보: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("사용자 위치 업데이트 오류: {}", e.getMessage());
            throw e;
        }
    }
    
    /**
     * 프로필 이미지 업데이트
     */
    @PutMapping("/{userId}/profile-image")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "프로필 이미지 업데이트", description = "사용자의 프로필 이미지를 업데이트합니다.")
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "프로필 이미지 업데이트 성공"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음"),
        @ApiResponse(responseCode = "400", description = "잘못된 이미지 URL"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Void> updateProfileImage(
            @Parameter(description = "사용자 ID", required = true) @PathVariable String userId,
            @Parameter(description = "프로필 이미지 URL", required = true) @RequestParam String profileImageUrl) {
        log.info("프로필 이미지 업데이트: 사용자ID={}, 이미지URL={}", userId, profileImageUrl);
        
        try {
            userService.updateProfileImage(userId, profileImageUrl);
            return ResponseEntity.ok().build();
        } catch (UserNotFoundException e) {
            log.error("사용자를 찾을 수 없음: {}", e.getMessage());
            throw e;
        } catch (IllegalArgumentException e) {
            log.error("잘못된 이미지 URL: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("프로필 이미지 업데이트 오류: {}", e.getMessage());
            throw e;
        }
    }
    
    /**
     * 사용자 통계 조회
     */
    @GetMapping("/statistics")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "사용자 통계 조회", description = "사용자 관련 통계 정보를 조회합니다.")
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "통계 조회 성공",
            content = @Content(schema = @Schema(implementation = UserDto.UserStats.class))),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<UserDto.UserStats> getUserStatistics() {
        log.info("사용자 통계 조회");
        
        try {
            UserDto.UserStats stats = userService.getUserStatistics();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("사용자 통계 조회 오류: {}", e.getMessage());
            throw e;
        }
    }
} 