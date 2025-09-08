package com.carecode.domain.user.controller;

import com.carecode.core.annotation.LogExecutionTime;
import com.carecode.core.controller.BaseController;
import com.carecode.core.exception.UserNotFoundException;
import com.carecode.domain.user.dto.UserDto;
import com.carecode.domain.user.dto.UserProfileUpdateDto;
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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 사용자 프로필 관리 API 컨트롤러
 * 카카오 로그인 후 추가 정보 입력 및 프로필 관리
 */
@Slf4j
@RestController
@RequestMapping("/api/user/profile")
@RequiredArgsConstructor
@Tag(name = "사용자 프로필", description = "사용자 프로필 관리 API (카카오 로그인 후 추가 정보 입력)")
@SecurityRequirement(name = "bearerAuth")
public class UserProfileController extends BaseController {

    private final UserService userService;
    private final JwtService jwtService;

    /**
     * 현재 사용자 프로필 조회
     */
    @GetMapping
    @LogExecutionTime
    @Operation(summary = "프로필 조회", description = "현재 로그인한 사용자의 프로필 정보를 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "프로필 조회 성공",
            content = @Content(schema = @Schema(implementation = UserDto.class))),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    public ResponseEntity<UserDto> getCurrentUserProfile() {
        UserInfo userInfo = getCurrentUserInfo();
        log.info("프로필 조회 요청: 사용자={} (이메일: {})", userInfo.getName(), userInfo.getEmail());
        
        try {
            // 이메일로 사용자 조회 (더 정확함)
            UserDto userDto = userService.getUserByEmail(userInfo.getEmail());
            return ResponseEntity.ok(userDto);
        } catch (UserNotFoundException e) {
            log.error("사용자를 찾을 수 없음: name={}, email={}", userInfo.getName(), userInfo.getEmail());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 프로필 완성도 체크
     */
    @GetMapping("/completion")
    @LogExecutionTime
    @Operation(summary = "프로필 완성도 체크", description = "사용자 프로필의 완성도를 확인합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "완성도 확인 성공",
            content = @Content(schema = @Schema(implementation = UserProfileUpdateDto.ProfileCompletionResponse.class))),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    public ResponseEntity<UserProfileUpdateDto.ProfileCompletionResponse> checkProfileCompletion() {
        UserInfo userInfo = getCurrentUserInfo();
        log.info("프로필 완성도 체크 요청: 사용자={} (이메일: {})", userInfo.getName(), userInfo.getEmail());
        
        try {
            User user = userService.getUserEntityByEmail(userInfo.getEmail());
            UserProfileUpdateDto.ProfileCompletionResponse completion = calculateProfileCompletion(user);
            return ResponseEntity.ok(completion);
        } catch (UserNotFoundException e) {
            log.error("사용자를 찾을 수 없음: name={}, email={}", userInfo.getName(), userInfo.getEmail());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 프로필 업데이트 (추가 정보 입력)
     */
    @PutMapping
    @LogExecutionTime
    @Operation(summary = "프로필 업데이트", description = "사용자의 추가 정보를 입력/업데이트합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "프로필 업데이트 성공",
            content = @Content(schema = @Schema(implementation = UserDto.class))),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    public ResponseEntity<UserDto> updateProfile(
            @Parameter(description = "업데이트할 프로필 정보", required = true)
            @Valid @RequestBody UserProfileUpdateDto updateDto) {
        
        UserInfo userInfo = getCurrentUserInfo();
        log.info("프로필 업데이트 요청: 사용자={} (이메일: {})", userInfo.getName(), userInfo.getEmail());
        
        try {
            User user = userService.getUserEntityByEmail(userInfo.getEmail());
            
            // 프로필 정보 업데이트
            updateUserProfile(user, updateDto);
            user.setUpdatedAt(LocalDateTime.now());
            
            // 저장
            User updatedUser = userService.saveUser(user);
            
            // DTO 변환 후 응답
            UserDto userDto = convertToDto(updatedUser);
            
            log.info("프로필 업데이트 완료: 사용자={} (이메일: {})", userInfo.getName(), userInfo.getEmail());
            return ResponseEntity.ok(userDto);
            
        } catch (UserNotFoundException e) {
            log.error("사용자를 찾을 수 없음: name={}, email={}", userInfo.getName(), userInfo.getEmail());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 닉네임 업데이트 (카카오 닉네임과 별도)
     */
    @PatchMapping("/nickname")
    @LogExecutionTime
    @Operation(summary = "닉네임 업데이트", description = "사용자의 표시 닉네임을 업데이트합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "닉네임 업데이트 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    public ResponseEntity<Map<String, String>> updateNickname(
            @Parameter(description = "새로운 닉네임", required = true)
            @RequestBody Map<String, String> request) {
        
        String currentUserEmail = getCurrentUserEmail();
        String newNickname = request.get("nickname");
        
        if (newNickname == null || newNickname.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "닉네임은 필수입니다"));
        }
        
        if (newNickname.length() < 2 || newNickname.length() > 10) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "닉네임은 2-10자 사이여야 합니다"));
        }
        
        log.info("닉네임 업데이트 요청: 사용자={}, 새 닉네임={}", currentUserEmail, newNickname);
        
        try {
            User user = userService.getUserEntityByEmail(currentUserEmail);
            user.setName(newNickname.trim());
            user.setUpdatedAt(LocalDateTime.now());
            userService.saveUser(user);
            
            log.info("닉네임 업데이트 완료: 사용자={}, 새 닉네임={}", currentUserEmail, newNickname);
            return ResponseEntity.ok(Map.of(
                    "message", "닉네임이 업데이트되었습니다",
                    "nickname", newNickname.trim()
            ));
            
        } catch (UserNotFoundException e) {
            log.error("사용자를 찾을 수 없음: {}", currentUserEmail);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 프로필 완성도 계산
     */
    private UserProfileUpdateDto.ProfileCompletionResponse calculateProfileCompletion(User user) {
        UserProfileUpdateDto.ProfileCompletionResponse.MissingFields missingFields = 
                UserProfileUpdateDto.ProfileCompletionResponse.MissingFields.builder()
                        .needsRealName(isBlank(user.getName()) || user.getName().contains("_"))
                        .needsPhoneNumber(isBlank(user.getPhoneNumber()))
                        .needsBirthDate(user.getBirthDate() == null)
                        .needsGender(user.getGender() == null)
                        .needsAddress(isBlank(user.getAddress()))
                        .build();

        int totalFields = 5;
        int completedFields = 0;
        
        if (!missingFields.isNeedsRealName()) completedFields++;
        if (!missingFields.isNeedsPhoneNumber()) completedFields++;
        if (!missingFields.isNeedsBirthDate()) completedFields++;
        if (!missingFields.isNeedsGender()) completedFields++;
        if (!missingFields.isNeedsAddress()) completedFields++;
        
        int percentage = (completedFields * 100) / totalFields;
        boolean isComplete = completedFields == totalFields;
        
        String message;
        if (isComplete) {
            message = "프로필이 완성되었습니다!";
        } else {
            message = String.format("프로필 완성도: %d%% (%d개 항목 추가 필요)", percentage, totalFields - completedFields);
        }
        
        return UserProfileUpdateDto.ProfileCompletionResponse.builder()
                .isComplete(isComplete)
                .completionPercentage(percentage)
                .message(message)
                .missingFields(missingFields)
                .build();
    }

    /**
     * 사용자 프로필 업데이트
     */
    private void updateUserProfile(User user, UserProfileUpdateDto updateDto) {
        if (!isBlank(updateDto.getRealName())) {
            user.setName(updateDto.getRealName().trim());
        }
        if (!isBlank(updateDto.getPhoneNumber())) {
            user.setPhoneNumber(updateDto.getPhoneNumber().trim());
        }
        if (updateDto.getBirthDate() != null) {
            user.setBirthDate(updateDto.getBirthDate());
        }
        if (updateDto.getGender() != null) {
            user.setGender(updateDto.getGender());
        }
        if (!isBlank(updateDto.getAddress())) {
            user.setAddress(updateDto.getAddress().trim());
        }
        if (updateDto.getLatitude() != null) {
            user.setLatitude(updateDto.getLatitude());
        }
        if (updateDto.getLongitude() != null) {
            user.setLongitude(updateDto.getLongitude());
        }
    }

    /**
     * User Entity를 UserDto로 변환
     */
    private UserDto convertToDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .userId(user.getUserId())
                .email(user.getEmail())
                .name(user.getName())
                .phoneNumber(user.getPhoneNumber())
                .birthDate(user.getBirthDate())
                .gender(user.getGender() != null ? user.getGender().name() : null)
                .address(user.getAddress())
                .latitude(user.getLatitude())
                .longitude(user.getLongitude())
                .profileImageUrl(user.getProfileImageUrl())
                .role(user.getRole().name())
                .isActive(user.getIsActive())
                .emailVerified(user.getEmailVerified())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    /**
     * 현재 로그인한 사용자의 이메일 추출
     */
    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("인증되지 않은 사용자입니다");
        }
        
        // JWT 토큰에서 이메일 추출
        String email = authentication.getName();
        log.debug("현재 사용자 이메일: {}", email);
        
        // email이 null이거나 비어있는 경우 예외 처리
        if (email == null || email.trim().isEmpty()) {
            throw new RuntimeException("토큰에서 이메일을 추출할 수 없습니다");
        }
        
        return email;
    }

    /**
     * 현재 로그인한 사용자의 이름과 이메일 추출
     */
    private UserInfo getCurrentUserInfo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("인증되지 않은 사용자입니다");
        }
        
        // JWT 토큰에서 이메일 추출 (principal이 email로 설정됨)
        String email = authentication.getName();
        
        if (email == null || !email.contains("@")) {
            log.error("유효하지 않은 이메일 형식: {}", email);
            throw new RuntimeException("유효하지 않은 사용자 정보입니다");
        }
        
        // 이메일로 사용자 정보 조회
        try {
            User user = userService.getUserEntityByEmail(email);
            String name = user.getName();
            
            log.debug("현재 사용자 정보: name={}, email={}", name, email);
            return new UserInfo(name, email);
        } catch (UserNotFoundException e) {
            log.error("사용자를 찾을 수 없음: {}", email);
            throw new RuntimeException("사용자 정보를 찾을 수 없습니다");
        }
    }

    /**
     * 사용자 정보를 담는 내부 클래스
     */
    private static class UserInfo {
        private final String name;
        private final String email;
        
        public UserInfo(String name, String email) {
            this.name = name;
            this.email = email;
        }
        
        public String getName() { return name; }
        public String getEmail() { return email; }
    }

    /**
     * JWT 토큰에서 직접 이메일 추출 (대안 방법)
     */
    private String getCurrentUserEmailFromToken(HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new RuntimeException("유효한 Authorization 헤더가 없습니다.");
            }
            
            String token = authHeader.substring(7);
            return jwtService.extractEmailFromToken(token);
        } catch (Exception e) {
            log.error("JWT 토큰에서 이메일 추출 실패: {}", e.getMessage());
            throw new RuntimeException("인증 토큰이 유효하지 않습니다.");
        }
    }

    /**
     * 문자열이 비어있는지 확인
     */
    private boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }
}