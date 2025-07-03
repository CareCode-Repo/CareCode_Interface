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
public class UserController extends BaseController {
    
    private final UserService userService;
    private static final String SUCCESS_MESSAGE = "처리가 완료되었습니다.";
    
    /**
     * 사용자 프로필 조회
     */
    @GetMapping("/{userId}")
    @LogExecutionTime
    @RequireAuthentication
    public ResponseEntity<UserDto> getUserProfile(@PathVariable String userId) {
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
    public ResponseEntity<UserDto> updateUserProfile(
            @PathVariable String userId,
            @RequestBody UserDto userDto) {
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
    public ResponseEntity<UserDto> createUser(@RequestBody UserDto userDto) {
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
    public ResponseEntity<Map<String, String>> deleteUser(@PathVariable String userId) {
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
    public ResponseEntity<List<UserDto>> searchUsers(@RequestParam String keyword) {
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
    public ResponseEntity<UserDto> getUserByEmail(@PathVariable String email) {
        log.info("사용자 조회 요청 - 이메일: {}", email);
        
        try {
            UserDto user = userService.getUserByEmail(email);
            return ResponseEntity.ok(user);
        } catch (UserNotFoundException e) {
            log.error("사용자를 찾을 수 없음: {}", e.getMessage());
            throw e;
        }
    }
    
    /**
     * 활성화된 사용자 목록 조회
     */
    @GetMapping("/active")
    @LogExecutionTime
    @RequireAuthentication
    public ResponseEntity<List<UserDto>> getActiveUsers() {
        log.info("활성화된 사용자 목록 조회 요청");
        
        try {
            List<UserDto> users = userService.getActiveUsers();
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            log.error("활성화된 사용자 목록 조회 오류: {}", e.getMessage());
            throw e;
        }
    }
    
    /**
     * 사용자 유형별 조회
     */
    @GetMapping("/type/{userType}")
    @LogExecutionTime
    @RequireAuthentication
    public ResponseEntity<List<UserDto>> getUsersByType(@PathVariable String userType) {
        log.info("사용자 유형별 조회 요청 - 유형: {}", userType);
        
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
    public ResponseEntity<List<UserDto>> getUsersByRegion(@PathVariable String region) {
        log.info("지역별 사용자 조회 요청 - 지역: {}", region);
        
        try {
            List<UserDto> users = userService.getUsersByRegion(region);
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            log.error("지역별 사용자 조회 오류: {}", e.getMessage());
            throw e;
        }
    }
    
    /**
     * 인증된 사용자 목록 조회
     */
    @GetMapping("/verified")
    @LogExecutionTime
    @RequireAuthentication
    public ResponseEntity<List<UserDto>> getVerifiedUsers() {
        log.info("인증된 사용자 목록 조회 요청");
        
        try {
            List<UserDto> users = userService.getVerifiedUsers();
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            log.error("인증된 사용자 목록 조회 오류: {}", e.getMessage());
            throw e;
        }
    }
    
    /**
     * 최근 로그인한 사용자 목록 조회
     */
    @GetMapping("/recent")
    @LogExecutionTime
    @RequireAuthentication
    public ResponseEntity<List<UserDto>> getRecentlyActiveUsers() {
        log.info("최근 로그인한 사용자 목록 조회 요청");
        
        try {
            List<UserDto> users = userService.getRecentlyActiveUsers();
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            log.error("최근 로그인한 사용자 목록 조회 오류: {}", e.getMessage());
            throw e;
        }
    }
    
    /**
     * 사용자 로그인
     */
    @PostMapping("/login")
    @LogExecutionTime
    public ResponseEntity<UserDto> login(@RequestBody LoginRequestDto request) {
        log.info("사용자 로그인 요청: 이메일={}", request.getEmail());
        
        try {
            UserDto user = userService.login(request.getEmail(), request.getPassword());
            return ResponseEntity.ok(user);
        } catch (UserNotFoundException e) {
            log.error("사용자를 찾을 수 없음: {}", e.getMessage());
            throw e;
        } catch (IllegalArgumentException e) {
            log.error("로그인 실패: {}", e.getMessage());
            throw e;
        }
    }
    
    /**
     * 비밀번호 변경
     */
    @PutMapping("/{userId}/password")
    @LogExecutionTime
    @RequireAuthentication
    public ResponseEntity<Void> changePassword(@PathVariable String userId, @RequestBody PasswordChangeRequestDto request) {
        log.info("비밀번호 변경 요청: 사용자ID={}", userId);
        
        try {
            userService.changePassword(userId, request.getOldPassword(), request.getNewPassword());
            return ResponseEntity.ok().build();
        } catch (UserNotFoundException e) {
            log.error("사용자를 찾을 수 없음: {}", e.getMessage());
            throw e;
        } catch (IllegalArgumentException e) {
            log.error("비밀번호 변경 실패: {}", e.getMessage());
            throw e;
        }
    }
    
    /**
     * 사용자 비활성화
     */
    @PutMapping("/{userId}/deactivate")
    @LogExecutionTime
    @RequireAuthentication
    public ResponseEntity<Void> deactivateUser(@PathVariable String userId) {
        log.info("사용자 비활성화 요청: 사용자ID={}", userId);
        
        try {
            userService.deactivateUser(userId);
            return ResponseEntity.ok().build();
        } catch (UserNotFoundException e) {
            log.error("사용자를 찾을 수 없음: {}", e.getMessage());
            throw e;
        } catch (IllegalArgumentException e) {
            log.error("사용자 비활성화 실패: {}", e.getMessage());
            throw e;
        }
    }
    
    /**
     * 사용자 활성화
     */
    @PutMapping("/{userId}/activate")
    @LogExecutionTime
    @RequireAuthentication
    public ResponseEntity<Void> activateUser(@PathVariable String userId) {
        log.info("사용자 활성화 요청: 사용자ID={}", userId);
        
        try {
            userService.activateUser(userId);
            return ResponseEntity.ok().build();
        } catch (UserNotFoundException e) {
            log.error("사용자를 찾을 수 없음: {}", e.getMessage());
            throw e;
        } catch (IllegalArgumentException e) {
            log.error("사용자 활성화 실패: {}", e.getMessage());
            throw e;
        }
    }
    
    /**
     * 사용자 인증
     */
    @PutMapping("/{userId}/verify")
    @LogExecutionTime
    @RequireAuthentication
    public ResponseEntity<Void> verifyUser(@PathVariable String userId) {
        log.info("사용자 인증 요청: 사용자ID={}", userId);
        
        try {
            userService.verifyUser(userId);
            return ResponseEntity.ok().build();
        } catch (UserNotFoundException e) {
            log.error("사용자를 찾을 수 없음: {}", e.getMessage());
            throw e;
        } catch (IllegalArgumentException e) {
            log.error("사용자 인증 실패: {}", e.getMessage());
            throw e;
        }
    }
    
    /**
     * 사용자 위치 업데이트
     */
    @PutMapping("/{userId}/location")
    @LogExecutionTime
    @RequireAuthentication
    public ResponseEntity<Void> updateUserLocation(
            @PathVariable String userId,
            @RequestParam Double latitude,
            @RequestParam Double longitude) {
        log.info("사용자 위치 업데이트 요청: 사용자ID={}, 위도={}, 경도={}", userId, latitude, longitude);
        
        try {
            userService.updateUserLocation(userId, latitude, longitude);
            return ResponseEntity.ok().build();
        } catch (UserNotFoundException e) {
            log.error("사용자를 찾을 수 없음: {}", e.getMessage());
            throw e;
        } catch (IllegalArgumentException e) {
            log.error("사용자 위치 업데이트 실패: {}", e.getMessage());
            throw e;
        }
    }
    
    /**
     * 프로필 이미지 업데이트
     */
    @PutMapping("/{userId}/profile-image")
    @LogExecutionTime
    @RequireAuthentication
    public ResponseEntity<Void> updateProfileImage(
            @PathVariable String userId,
            @RequestParam String profileImageUrl) {
        log.info("프로필 이미지 업데이트 요청: 사용자ID={}", userId);
        
        try {
            userService.updateProfileImage(userId, profileImageUrl);
            return ResponseEntity.ok().build();
        } catch (UserNotFoundException e) {
            log.error("사용자를 찾을 수 없음: {}", e.getMessage());
            throw e;
        } catch (IllegalArgumentException e) {
            log.error("프로필 이미지 업데이트 실패: {}", e.getMessage());
            throw e;
        }
    }
    
    /**
     * 사용자 통계 조회
     */
    @GetMapping("/statistics")
    @LogExecutionTime
    @RequireAuthentication
    public ResponseEntity<UserDto.UserStats> getUserStatistics() {
        log.info("사용자 통계 조회 요청");
        
        try {
            UserDto.UserStats stats = userService.getUserStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("사용자 통계 조회 오류: {}", e.getMessage());
            throw e;
        }
    }
} 