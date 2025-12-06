package com.carecode.domain.user.controller;

import com.carecode.core.annotation.LogExecutionTime;
import com.carecode.core.annotation.RequireAuthentication;
import com.carecode.core.controller.BaseController;
import com.carecode.domain.user.dto.response.UserDto;
import com.carecode.domain.user.dto.response.UserProfileUpdateDto;
import com.carecode.domain.user.dto.response.UserProfileCompletionResponse;
import com.carecode.domain.user.dto.response.UserProfileMissingFields;
import com.carecode.domain.user.dto.request.UserUpdateRequestDto;
import com.carecode.domain.user.entity.User;
import com.carecode.domain.user.service.UserService;
import com.carecode.domain.user.app.UserFacade;
import com.carecode.domain.user.mapper.UserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.carecode.core.handler.ApiSuccess;
import com.carecode.domain.user.dto.response.UserResponse;
import com.carecode.domain.user.dto.response.UserStatsResponse;
import com.carecode.domain.user.dto.response.UserSearchResponse;
import com.carecode.domain.user.dto.response.UserListResponse;
import com.carecode.domain.user.dto.response.UserInfoResponse;

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
public class UserController extends BaseController {

    private final UserService userService;
    private final UserFacade userFacade;
    private final UserMapper userMapper;

    // ==================== 사용자 프로필 ====================

    /**
     * 사용자 프로필 조회
     */
    @GetMapping("/{userId}")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "사용자 프로필 조회", description = "특정 사용자의 프로필 정보를 조회합니다.")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<UserDto> getUserProfile(@Parameter(description = "사용자 ID", required = true)
                                                      @PathVariable String userId) {
        UserDto user = userFacade.getUserById(userId);
        return ResponseEntity.ok(user);
    }

    /**
     * 현재 사용자 프로필 조회
     */
    @GetMapping("/profile")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "현재 사용자 프로필 조회", description = "현재 로그인한 사용자의 프로필 정보를 조회합니다.")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<UserDto> getCurrentUserProfile() {
        String currentUserEmail = getCurrentUserEmail();
        UserDto userDto = userFacade.getUserByEmail(currentUserEmail);

        return ResponseEntity.ok(userDto);
    }

    /**
     * 사용자 프로필 업데이트
     */
    @PutMapping("/{userId}")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "사용자 프로필 업데이트", description = "사용자의 프로필 정보를 업데이트합니다.")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<UserDto> updateUserProfile(@Parameter(description = "사용자 ID", required = true) @PathVariable String userId,
                                                     @Parameter(description = "업데이트할 사용자 정보", required = true)
                                                     @Valid @RequestBody UserUpdateRequestDto request) {
        User user = userFacade.getUserEntityByEmail(getCurrentUserEmail());
        userMapper.updateUserFromRequest(request, user);
        UserDto updated = userMapper.toDto(userService.saveUser(user));
        return ResponseEntity.ok(updated);
    }

    /**
     * 프로필 완성도 체크
     */
    @GetMapping("/profile/completion")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "프로필 완성도 체크", description = "사용자 프로필의 완성도를 확인합니다.")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<UserProfileCompletionResponse> checkProfileCompletion() {
        String currentUserEmail = getCurrentUserEmail();
        User user = userFacade.getUserEntityByEmail(currentUserEmail);
        UserProfileCompletionResponse completion = calculateProfileCompletion(user);
        return ResponseEntity.ok(completion);
    }

    /**
     * 프로필 이미지 업데이트
     */
    @PutMapping("/{userId}/profile-image")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "프로필 이미지 업데이트", description = "사용자의 프로필 이미지를 업데이트합니다.")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<Void> updateProfileImage(@Parameter(description = "사용자 ID", required = true) @PathVariable String userId,
                                                   @Parameter(description = "프로필 이미지 URL", required = true) @RequestParam String profileImageUrl) {
        userFacade.updateProfileImage(userId, profileImageUrl);
        return ResponseEntity.ok().build();
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
    public ResponseEntity<UserDto> updateUserLocation(@Parameter(description = "사용자 ID", required = true) @PathVariable String userId,
                                                      @Parameter(description = "위도", required = true) @RequestParam Double latitude,
                                                      @Parameter(description = "경도", required = true) @RequestParam Double longitude) {
        UserDto updatedUser = userFacade.updateUserLocation(userId, latitude, longitude);
        return ResponseEntity.ok(updatedUser);
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
    public ResponseEntity<ApiSuccess> deactivateUser(@Parameter(description = "사용자 ID", required = true) @PathVariable String userId) {
        userFacade.deactivateUser(userId);
        return ResponseEntity.ok(ApiSuccess.of("회원 탈퇴가 완료되었습니다."));
    }

    /**
     * 회원 탈퇴 (소프트 삭제)
     */
    @DeleteMapping("/{userId}")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "회원 탈퇴 (소프트 삭제)", description = "사용자 계정을 소프트 삭제합니다. 데이터는 보존되며 필요시 복구 가능합니다.")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<ApiSuccess> deleteUser(@Parameter(description = "사용자 ID", required = true) @PathVariable String userId) {
        userFacade.deleteUser(userId);
        return ResponseEntity.ok(ApiSuccess.of("회원 탈퇴가 완료되었습니다. 데이터는 보존되며 필요시 복구 가능합니다."));
    }

    /**
     * 계정 복구 (비활성화된 계정 재활성화)
     */
    @PutMapping("/{userId}/reactivate")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "계정 복구", description = "비활성화된 사용자 계정을 재활성화합니다.")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<ApiSuccess> reactivateUser(@Parameter(description = "사용자 ID", required = true) @PathVariable String userId) {
        userFacade.reactivateUser(userId);
        return ResponseEntity.ok(ApiSuccess.of("계정이 성공적으로 복구되었습니다."));
    }

    // ==================== 프로필 관리 ====================

    // (중복 제거) 현재 사용자 프로필 조회는 상단의 userFacade 사용 버전만 유지

    // (중복 제거) 프로필 완성도 체크는 상단의 userFacade 사용 버전만 유지

    /**
     * 프로필 업데이트 (추가 정보 입력)
     */
    @PutMapping("/profile")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "프로필 업데이트", description = "사용자의 추가 정보를 입력/업데이트합니다.")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<UserDto> updateProfile(@Parameter(description = "업데이트할 프로필 정보", required = true)
                                                     @Valid @RequestBody UserUpdateRequestDto updateDto) {
        String currentUserEmail = getCurrentUserEmail();
        User user = userService.getUserEntityByEmail(currentUserEmail);

        // 프로필 정보 업데이트
        updateUserProfile(user, updateDto);
        user.setUpdatedAt(LocalDateTime.now());

        // 저장
        User updatedUser = userService.saveUser(user);

        // DTO 변환 후 응답
        UserDto userDto = convertToDto(updatedUser);
        return ResponseEntity.ok(userDto);
    }

    /**
     * 닉네임 업데이트 (카카오 닉네임과 별도)
     */
    @PatchMapping("/profile/nickname")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "닉네임 업데이트", description = "사용자의 표시 닉네임을 업데이트합니다.")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<ApiSuccess> updateNickname(@Parameter(description = "새로운 닉네임", required = true)
                                                                  @RequestBody Map<String, String> request) {
        String currentUserEmail = getCurrentUserEmail();
        String newNickname = request.get("nickname");
        
        if (newNickname == null || newNickname.trim().isEmpty()) {
            throw new IllegalArgumentException("닉네임은 필수입니다");
        }
        
        if (newNickname.length() < 2 || newNickname.length() > 10) {
            throw new IllegalArgumentException("닉네임은 2-10자 사이여야 합니다");
        }

        User user = userService.getUserEntityByEmail(currentUserEmail);
        user.setName(newNickname.trim());
        user.setUpdatedAt(LocalDateTime.now());
        userService.saveUser(user);

        return ResponseEntity.ok(ApiSuccess.of("닉네임이 업데이트되었습니다"));
    }

    // ==================== 사용자 관리 (관리자용) ====================

    /**
     * 사용자 통계 조회
     */
    @GetMapping("/statistics")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "사용자 통계 조회", description = "전체 사용자 통계를 조회합니다.")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<UserStatsResponse> getUserStatistics() {
        UserStatsResponse stats = userService.getUserStatistics();
        return ResponseEntity.ok(stats);
    }

    /**
     * 사용자 검색
     */
    @GetMapping("/search")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "사용자 검색", description = "키워드로 사용자를 검색합니다.")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<UserSearchResponse> searchUsers(@Parameter(description = "검색 키워드", required = true) @RequestParam String keyword,
                                                               @Parameter(description = "검색 타입", required = false) @RequestParam(required = false) String type) {
        
        List<UserDto> users;
        if (type != null && !type.isEmpty()) {
            users = userService.searchUsers(keyword, type);
        } else {
            users = userService.searchUsers(keyword);
        }
        
        UserSearchResponse searchResult = UserSearchResponse.builder()
                .users(users.stream().map(this::convertToUserInfo).collect(Collectors.toList()))
                .totalCount(users.size())
                .searchKeyword(keyword)
                .searchFilters(type != null ? List.of(type) : List.of())
                .build();
        
        return ResponseEntity.ok(searchResult);
    }

    /**
     * 활성 사용자 목록 조회
     */
    @GetMapping("/active")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "활성 사용자 목록", description = "활성화된 사용자 목록을 조회합니다.")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<UserListResponse> getActiveUsers() {
        List<UserDto> users = userService.getActiveUsers();
        UserListResponse userList = UserListResponse.builder()
                .users(users.stream().map(this::convertToUserInfo).collect(Collectors.toList()))
                .totalCount(users.size())
                .currentPage(0)
                .totalPages(1)
                .hasNext(false)
                .hasPrevious(false)
                .build();
        return ResponseEntity.ok(userList);
    }

    /**
     * 사용자 유형별 조회
     */
    @GetMapping("/by-type/{userType}")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "사용자 유형별 조회", description = "특정 유형의 사용자들을 조회합니다.")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<UserListResponse> getUsersByType(@Parameter(description = "사용자 유형", required = true) @PathVariable String userType) {
        List<UserDto> users = userService.getUsersByType(userType);
        UserListResponse userList = UserListResponse.builder()
                .users(users.stream().map(this::convertToUserInfo).collect(Collectors.toList()))
                .totalCount(users.size())
                .currentPage(0)
                .totalPages(1)
                .hasNext(false)
                .hasPrevious(false)
                .build();
        return ResponseEntity.ok(userList);
    }

    /**
     * 지역별 사용자 조회
     */
    @GetMapping("/by-region/{region}")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "지역별 사용자 조회", description = "특정 지역의 사용자들을 조회합니다.")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<UserListResponse> getUsersByRegion(@Parameter(description = "지역", required = true) @PathVariable String region) {
        List<UserDto> users = userService.getUsersByRegion(region);
        UserListResponse userList = UserListResponse.builder()
                .users(users.stream().map(this::convertToUserInfo).collect(Collectors.toList()))
                .totalCount(users.size())
                .currentPage(0)
                .totalPages(1)
                .hasNext(false)
                .hasPrevious(false)
                .build();
        return ResponseEntity.ok(userList);
    }

    /**
     * 인증된 사용자 목록 조회
     */
    @GetMapping("/verified")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "인증된 사용자 목록", description = "이메일 인증이 완료된 사용자 목록을 조회합니다.")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<UserListResponse> getVerifiedUsers() {
        List<UserDto> users = userService.getVerifiedUsers();
        UserListResponse userList = UserListResponse.builder()
                .users(users.stream().map(this::convertToUserInfo).collect(Collectors.toList()))
                .totalCount(users.size())
                .currentPage(0)
                .totalPages(1)
                .hasNext(false)
                .hasPrevious(false)
                .build();
        return ResponseEntity.ok(userList);
    }

    /**
     * 최근 활동 사용자 목록 조회
     */
    @GetMapping("/recently-active")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "최근 활동 사용자 목록", description = "최근에 활동한 사용자 목록을 조회합니다.")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<UserListResponse> getRecentlyActiveUsers() {
        List<UserDto> users = userService.getRecentlyActiveUsers();
        UserListResponse userList = UserListResponse.builder()
                .users(users.stream().map(this::convertToUserInfo).collect(Collectors.toList()))
                .totalCount(users.size())
                .currentPage(0)
                .totalPages(1)
                .hasNext(false)
                .hasPrevious(false)
                .build();
        return ResponseEntity.ok(userList);
    }

    /**
     * 사용자 역할 변경
     */
    @PutMapping("/{userId}/role")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "사용자 역할 변경", description = "사용자의 역할을 변경합니다.")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<ApiSuccess> updateUserRole(@Parameter(description = "사용자 ID", required = true) @PathVariable String userId,
                                                     @Parameter(description = "새로운 역할", required = true) @RequestBody Map<String, String> request) {
        String newRole = request.get("role");
        if (newRole == null || newRole.trim().isEmpty()) {
            throw new IllegalArgumentException("역할은 필수입니다");
        }
        
        try {
            Long id = Long.parseLong(userId);
            userService.updateUserRole(id, newRole);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("잘못된 사용자 ID 형식입니다: " + userId);
        }
        
        return ResponseEntity.ok(ApiSuccess.of("사용자 역할이 변경되었습니다"));
    }

    /**
     * 사용자 활성화
     */
    @PutMapping("/{userId}/activate")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "사용자 활성화", description = "비활성화된 사용자를 활성화합니다.")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<ApiSuccess> activateUser(@Parameter(description = "사용자 ID", required = true) @PathVariable String userId) {
        userService.activateUser(userId);
        return ResponseEntity.ok(ApiSuccess.of("사용자가 활성화되었습니다"));
    }

    // ==================== 유틸리티 메서드 ====================

    /**
     * 현재 로그인한 사용자의 이메일을 가져오기
     */
    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("인증 정보가 없습니다. 로그인 후 다시 시도하세요.");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }
        if (principal instanceof String s) {
            return s;
        }
        return authentication.getName();
    }

    /**
     * 사용자 프로필 업데이트
     */
    private void updateUserProfile(User user, UserUpdateRequestDto updateDto) {
        if (!isBlank(updateDto.getName())) {
            user.setName(updateDto.getName().trim());
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
                .role(user.getRole() != null ? user.getRole().name() : null)
                .provider(user.getProvider())
                .providerId(user.getProviderId())
                .isActive(user.getIsActive())
                .emailVerified(user.getEmailVerified())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    /**
     * 프로필 완성도 계산
     */
    private UserProfileCompletionResponse calculateProfileCompletion(User user) {
        UserProfileMissingFields missingFields = 
                UserProfileMissingFields.builder()
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
        
        return UserProfileCompletionResponse.builder()
                .isComplete(isComplete)
                .completionPercentage(percentage)
                .message(message)
                .missingFields(missingFields)
                .build();
    }

    /**
     * 문자열이 비어있는지 확인
     */
    private boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * UserDto를 UserInfoResponse로 변환
     */
    private UserInfoResponse convertToUserInfo(UserDto userDto) {
        return UserInfoResponse.builder()
                .id(userDto.getId())
                .userId(userDto.getUserId())
                .email(userDto.getEmail())
                .name(userDto.getName())
                .phoneNumber(userDto.getPhoneNumber())
                .birthDate(userDto.getBirthDate())
                .gender(userDto.getGender())
                .address(userDto.getAddress())
                .latitude(userDto.getLatitude())
                .longitude(userDto.getLongitude())
                .profileImageUrl(userDto.getProfileImageUrl())
                .role(userDto.getRole())
                .provider(userDto.getProvider())
                .providerId(userDto.getProviderId())
                .isActive(userDto.getIsActive())
                .emailVerified(userDto.getEmailVerified())
                .registrationCompleted(userDto.getRegistrationCompleted())
                .lastLoginAt(userDto.getLastLoginAt())
                .createdAt(userDto.getCreatedAt())
                .updatedAt(userDto.getUpdatedAt())
                .build();
    }
}
