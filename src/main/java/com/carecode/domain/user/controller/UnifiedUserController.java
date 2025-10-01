package com.carecode.domain.user.controller;

import com.carecode.core.annotation.LogExecutionTime;
import com.carecode.core.annotation.RequireAuthentication;
import com.carecode.core.controller.BaseController;
import com.carecode.core.exception.UserNotFoundException;
import com.carecode.domain.user.dto.UserDto;
import com.carecode.domain.user.dto.UserProfileUpdateDto;
import com.carecode.domain.user.entity.User;
import com.carecode.domain.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 통합 사용자 관리 컨트롤러
 * 사용자 프로필, 통계, 위치 관리 등 모든 사용자 관련 API
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", allowedHeaders = "*")
@Tag(name = "사용자 관리", description = "통합 사용자 관리 API (프로필, 통계, 위치)")
public class UnifiedUserController extends BaseController {

    private final UserService userService;

    // ==================== 사용자 프로필 ====================

    /**
     * 사용자 프로필 조회
     */
    @GetMapping("/{userId}")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "사용자 프로필 조회", description = "특정 사용자의 프로필 정보를 조회합니다.")
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
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
     * 현재 사용자 프로필 조회
     */
    @GetMapping("/profile")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "현재 사용자 프로필 조회", description = "현재 로그인한 사용자의 프로필 정보를 조회합니다.")
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "프로필 조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    public ResponseEntity<UserDto> getCurrentUserProfile() {
        String currentUserEmail = getCurrentUserEmail();
        log.info("현재 사용자 프로필 조회: 사용자={}", currentUserEmail);
        
        try {
            UserDto userDto = userService.getUserByEmail(currentUserEmail);
            return ResponseEntity.ok(userDto);
        } catch (UserNotFoundException e) {
            log.error("사용자를 찾을 수 없음: {}", currentUserEmail);
            return ResponseEntity.notFound().build();
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
        @ApiResponse(responseCode = "200", description = "업데이트 성공"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청")
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
     * 프로필 완성도 체크
     */
    @GetMapping("/profile/completion")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "프로필 완성도 체크", description = "사용자 프로필의 완성도를 확인합니다.")
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "완성도 확인 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    public ResponseEntity<UserProfileUpdateDto.ProfileCompletionResponse> checkProfileCompletion() {
        String currentUserEmail = getCurrentUserEmail();
        log.info("프로필 완성도 체크: 사용자={}", currentUserEmail);
        
        try {
            User user = userService.getUserEntityByEmail(currentUserEmail);
            UserProfileUpdateDto.ProfileCompletionResponse completion = calculateProfileCompletion(user);
            return ResponseEntity.ok(completion);
        } catch (UserNotFoundException e) {
            log.error("사용자를 찾을 수 없음: {}", currentUserEmail);
            return ResponseEntity.notFound().build();
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
        @ApiResponse(responseCode = "400", description = "잘못된 이미지 URL")
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
        }
    }

    // ==================== 사용자 통계 ====================

    /**
     * 사용자 통계 조회
     */
    @GetMapping("/statistics")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "사용자 통계 조회", description = "사용자 관련 통계 정보를 조회합니다.")
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "통계 조회 성공"),
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

    // ==================== 사용자 위치 관리 ====================

    /**
     * 사용자 위치 업데이트
     */
    @PutMapping("/{userId}/location")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "사용자 위치 업데이트", description = "사용자의 현재 위치를 업데이트합니다.")
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "위치 업데이트 성공"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음"),
        @ApiResponse(responseCode = "400", description = "잘못된 위치 정보")
    })
    public ResponseEntity<UserDto> updateUserLocation(
            @Parameter(description = "사용자 ID", required = true) @PathVariable String userId,
            @Parameter(description = "위도", required = true) @RequestParam Double latitude,
            @Parameter(description = "경도", required = true) @RequestParam Double longitude) {
        log.info("사용자 위치 업데이트: 사용자ID={}, 위도={}, 경도={}", userId, latitude, longitude);
        
        try {
            UserDto updatedUser = userService.updateUserLocation(userId, latitude, longitude);
            return ResponseEntity.ok(updatedUser);
        } catch (UserNotFoundException e) {
            log.error("사용자를 찾을 수 없음: {}", e.getMessage());
            throw e;
        } catch (IllegalArgumentException e) {
            log.error("잘못된 위치 정보: {}", e.getMessage());
            throw e;
        }
    }

    // ==================== 사용자 검색 ====================

    /**
     * 사용자 검색
     */
    @GetMapping("/search")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "사용자 검색", description = "이름 또는 이메일로 사용자를 검색합니다.")
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "검색 성공"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "400", description = "잘못된 검색 조건")
    })
    public ResponseEntity<List<UserDto>> searchUsers(
            @Parameter(description = "검색 키워드", required = true) @RequestParam String keyword,
            @Parameter(description = "검색 타입 (name, email)", required = true) @RequestParam String type) {
        log.info("사용자 검색: 키워드={}, 타입={}", keyword, type);
        
        try {
            List<UserDto> users = userService.searchUsers(keyword, type);
            return ResponseEntity.ok(users);
        } catch (IllegalArgumentException e) {
            log.error("잘못된 검색 조건: {}", e.getMessage());
            throw e;
        }
    }

    // ==================== 회원 탈퇴 ====================

    /**
     * 회원 탈퇴 (계정 비활성화)
     */
    @PutMapping("/{userId}/deactivate")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "회원 탈퇴 (계정 비활성화)", description = "사용자 계정을 비활성화합니다. 데이터는 보존되며 필요시 복구 가능합니다.")
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "회원 탈퇴 성공"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음"),
        @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    public ResponseEntity<Map<String, String>> deactivateUser(
            @Parameter(description = "사용자 ID", required = true) @PathVariable String userId) {
        log.info("회원 탈퇴 요청: 사용자ID={}", userId);
        
        try {
            userService.deactivateUser(userId);
            return ResponseEntity.ok(Map.of(
                "message", "회원 탈퇴가 완료되었습니다.",
                "userId", userId,
                "status", "deactivated"
            ));
        } catch (UserNotFoundException e) {
            log.error("사용자를 찾을 수 없음: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("회원 탈퇴 중 오류 발생: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 회원 탈퇴 (소프트 삭제)
     */
    @DeleteMapping("/{userId}")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "회원 탈퇴 (소프트 삭제)", description = "사용자 계정을 소프트 삭제합니다. 데이터는 보존되며 필요시 복구 가능합니다.")
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "회원 탈퇴 성공"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음"),
        @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    public ResponseEntity<Map<String, String>> deleteUser(
            @Parameter(description = "사용자 ID", required = true) @PathVariable String userId) {
        log.info("회원 소프트 삭제 요청: 사용자ID={}", userId);
        
        try {
            userService.deleteUser(userId);
            return ResponseEntity.ok(Map.of(
                "message", "회원 탈퇴가 완료되었습니다. 데이터는 보존되며 필요시 복구 가능합니다.",
                "userId", userId,
                "status", "soft_deleted"
            ));
        } catch (UserNotFoundException e) {
            log.error("사용자를 찾을 수 없음: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("회원 탈퇴 중 오류 발생: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 계정 복구 (비활성화된 계정 재활성화)
     */
    @PutMapping("/{userId}/reactivate")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "계정 복구", description = "비활성화된 사용자 계정을 재활성화합니다.")
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "계정 복구 성공"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음"),
        @ApiResponse(responseCode = "400", description = "이미 활성화된 계정")
    })
    public ResponseEntity<Map<String, String>> reactivateUser(
            @Parameter(description = "사용자 ID", required = true) @PathVariable String userId) {
        log.info("계정 복구 요청: 사용자ID={}", userId);
        
        try {
            userService.reactivateUser(userId);
            return ResponseEntity.ok(Map.of(
                "message", "계정이 성공적으로 복구되었습니다.",
                "userId", userId,
                "status", "reactivated"
            ));
        } catch (UserNotFoundException e) {
            log.error("사용자를 찾을 수 없음: {}", e.getMessage());
            throw e;
        } catch (IllegalArgumentException e) {
            log.error("계정 복구 실패: {}", e.getMessage());
            throw e;
        }
    }

    // ==================== 유틸리티 메서드 ====================

    /**
     * 현재 로그인한 사용자의 이메일을 가져오기
     */
    private String getCurrentUserEmail() {
        // JWT 토큰에서 이메일 추출 로직
        // 실제 구현에서는 SecurityContext에서 가져와야 함
        return "current.user@example.com"; // 임시 구현
    }

    /**
     * 프로필 완성도 계산
     */
    private UserProfileUpdateDto.ProfileCompletionResponse calculateProfileCompletion(User user) {
        int totalFields = 8; // 총 필드 수
        int completedFields = 0;
        
        if (user.getName() != null && !user.getName().trim().isEmpty()) completedFields++;
        if (user.getEmail() != null && !user.getEmail().trim().isEmpty()) completedFields++;
        if (user.getPhoneNumber() != null && !user.getPhoneNumber().trim().isEmpty()) completedFields++;
        if (user.getBirthDate() != null) completedFields++;
        if (user.getGender() != null) completedFields++;
        if (user.getAddress() != null && !user.getAddress().trim().isEmpty()) completedFields++;
        if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().trim().isEmpty()) completedFields++;
        if (user.getRole() != null) completedFields++;
        
        int completionPercentage = (completedFields * 100) / totalFields;
        
        return UserProfileUpdateDto.ProfileCompletionResponse.builder()
                .completionPercentage(completionPercentage)
                .completedFields(completedFields)
                .totalFields(totalFields)
                .build();
    }
}
