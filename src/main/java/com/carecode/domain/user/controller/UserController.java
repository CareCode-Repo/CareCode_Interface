package com.carecode.domain.user.controller;

import com.carecode.core.annotation.LogExecutionTime;
import com.carecode.core.controller.BaseController;
import com.carecode.core.handler.ApiSuccess;
import com.carecode.core.security.CurrentUserFacade;
import com.carecode.domain.user.app.UserFacade;
import com.carecode.domain.user.dto.request.UserUpdateRequestDto;
import com.carecode.domain.user.dto.response.UserDto;
import com.carecode.domain.user.dto.response.UserProfileCompletionResponse;
import com.carecode.domain.user.dto.response.UserProfileMissingFields;
import com.carecode.domain.user.entity.User;
import com.carecode.domain.user.mapper.UserMapper;
import com.carecode.domain.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 본인 계정 전용 API.
 *
 * <p>이 컨트롤러의 모든 엔드포인트는 "로그인한 사람이 자기 자신에게" 하는 동작만 다룬다.
 * 예전에는 여기에 역할 변경·사용자 검색·전체 목록 조회 같은 관리 기능이 함께 있었고,
 * 클래스 레벨 제약이 isAuthenticated() 뿐이라 아무 회원이나
 * PUT /users/{id}/role 로 자신을 ADMIN 으로 올릴 수 있었다.
 * 관리 기능은 전부 /api/admin/users 로 옮겼다.
 *
 * <p>경로 변수 userId 가 남아 있는 엔드포인트는 기존 클라이언트 호환을 위해 형태만 유지하며,
 * 실제로는 {@link CurrentUserFacade#requireSelf} 로 본인인지 확인한 뒤 본인 엔티티로만 동작한다.
 * 신규 클라이언트는 /users/me/... 별칭을 사용한다.
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("isAuthenticated()")
@Tag(name = "사용자", description = "본인 계정 관리 API (프로필, 위치, 탈퇴)")
public class UserController extends BaseController {

    private final UserService userService;
    private final UserFacade userFacade;
    private final UserMapper userMapper;
    private final CurrentUserFacade currentUserFacade;

    // ==================== 프로필 조회 ====================

    @GetMapping({"/profile", "/me"})
    @LogExecutionTime
    @Operation(summary = "내 프로필 조회", description = "현재 로그인한 사용자의 프로필 정보 조회")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<UserDto> getCurrentUserProfile() {
        return ResponseEntity.ok(userFacade.getUserByEmail(getCurrentUserEmail()));
    }

    @GetMapping({"/profile/completion", "/me/profile-completion"})
    @LogExecutionTime
    @Operation(summary = "프로필 완성도 체크", description = "내 프로필의 완성도를 확인")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<UserProfileCompletionResponse> checkProfileCompletion() {
        User user = userFacade.getUserEntityByEmail(getCurrentUserEmail());
        return ResponseEntity.ok(calculateProfileCompletion(user));
    }

    // ==================== 프로필 수정 ====================

    @PutMapping({"/profile", "/me"})
    @LogExecutionTime
    @Operation(summary = "내 프로필 수정", description = "이름·연락처·생년월일·성별·주소 등을 수정")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<UserDto> updateProfile(
            @Parameter(description = "업데이트할 프로필 정보", required = true)
            @Valid @RequestBody UserUpdateRequestDto updateDto) {
        User user = userService.getUserEntityByEmail(getCurrentUserEmail());
        userMapper.updateUserFromRequest(updateDto, user);
        user.setUpdatedAt(LocalDateTime.now());
        return ResponseEntity.ok(userMapper.toDto(userService.saveUser(user)));
    }

    @PatchMapping({"/profile/nickname", "/me/nickname"})
    @LogExecutionTime
    @Operation(summary = "닉네임 변경", description = "표시 닉네임을 변경")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<ApiSuccess> updateNickname(
            @Parameter(description = "새로운 닉네임", required = true) @RequestBody Map<String, String> request) {
        String newNickname = request.get("nickname");

        if (newNickname == null || newNickname.trim().isEmpty()) {
            throw new IllegalArgumentException("닉네임은 필수입니다");
        }
        String trimmed = newNickname.trim();
        if (trimmed.length() < 2 || trimmed.length() > 10) {
            throw new IllegalArgumentException("닉네임은 2-10자 사이여야 합니다");
        }

        User user = userService.getUserEntityByEmail(getCurrentUserEmail());
        user.setName(trimmed);
        user.setUpdatedAt(LocalDateTime.now());
        userService.saveUser(user);

        return ResponseEntity.ok(ApiSuccess.of("닉네임이 업데이트되었습니다"));
    }

    @PutMapping("/me/profile-image")
    @LogExecutionTime
    @Operation(summary = "프로필 이미지 변경")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<Void> updateMyProfileImage(
            @Parameter(description = "프로필 이미지 URL", required = true) @RequestParam String profileImageUrl) {
        userFacade.updateProfileImage(selfDbId(), profileImageUrl);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{userId}/profile-image")
    @LogExecutionTime
    @Operation(summary = "프로필 이미지 변경 (구 경로)",
            description = "본인만 변경할 수 있습니다. 신규 클라이언트는 /users/me/profile-image 를 사용하세요")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<Void> updateProfileImage(
            @Parameter(description = "사용자 ID", required = true) @PathVariable String userId,
            @Parameter(description = "프로필 이미지 URL", required = true) @RequestParam String profileImageUrl) {
        User self = currentUserFacade.requireSelf(userId);
        userFacade.updateProfileImage(String.valueOf(self.getId()), profileImageUrl);
        return ResponseEntity.ok().build();
    }

    // ==================== 위치 ====================

    @PutMapping("/me/location")
    @LogExecutionTime
    @Operation(summary = "내 위치 갱신", description = "주변 시설 추천에 사용할 현재 위치를 갱신")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<UserDto> updateMyLocation(
            @Parameter(description = "위도", required = true) @RequestParam Double latitude,
            @Parameter(description = "경도", required = true) @RequestParam Double longitude) {
        return ResponseEntity.ok(userFacade.updateUserLocation(selfDbId(), latitude, longitude));
    }

    @PutMapping("/{userId}/location")
    @LogExecutionTime
    @Operation(summary = "위치 갱신 (구 경로)",
            description = "본인만 변경할 수 있습니다. 신규 클라이언트는 /users/me/location 을 사용하세요")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<UserDto> updateUserLocation(
            @Parameter(description = "사용자 ID", required = true) @PathVariable String userId,
            @Parameter(description = "위도", required = true) @RequestParam Double latitude,
            @Parameter(description = "경도", required = true) @RequestParam Double longitude) {
        User self = currentUserFacade.requireSelf(userId);
        return ResponseEntity.ok(userFacade.updateUserLocation(String.valueOf(self.getId()), latitude, longitude));
    }

    // ==================== 회원 탈퇴 ====================

    @PutMapping("/me/deactivate")
    @LogExecutionTime
    @Operation(summary = "내 계정 비활성화", description = "데이터는 보존되며 관리자를 통해 복구할 수 있습니다")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<ApiSuccess> deactivateMe() {
        userFacade.deactivateUser(selfDbId());
        return ResponseEntity.ok(ApiSuccess.of("회원 탈퇴가 완료되었습니다."));
    }

    @PutMapping("/{userId}/deactivate")
    @LogExecutionTime
    @Operation(summary = "계정 비활성화 (구 경로)", description = "본인만 요청할 수 있습니다")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<ApiSuccess> deactivateUser(
            @Parameter(description = "사용자 ID", required = true) @PathVariable String userId) {
        User self = currentUserFacade.requireSelf(userId);
        userFacade.deactivateUser(String.valueOf(self.getId()));
        return ResponseEntity.ok(ApiSuccess.of("회원 탈퇴가 완료되었습니다."));
    }

    @DeleteMapping("/me")
    @LogExecutionTime
    @Operation(summary = "회원 탈퇴 (소프트 삭제)", description = "데이터는 보존되며 필요시 복구 가능")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<ApiSuccess> deleteMe() {
        userFacade.deleteUser(selfDbId());
        return ResponseEntity.ok(ApiSuccess.of("회원 탈퇴가 완료되었습니다. 데이터는 보존되며 필요시 복구 가능합니다."));
    }

    @DeleteMapping("/{userId}")
    @LogExecutionTime
    @Operation(summary = "회원 탈퇴 (구 경로)", description = "본인만 요청할 수 있습니다")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<ApiSuccess> deleteUser(
            @Parameter(description = "사용자 ID", required = true) @PathVariable String userId) {
        User self = currentUserFacade.requireSelf(userId);
        userFacade.deleteUser(String.valueOf(self.getId()));
        return ResponseEntity.ok(ApiSuccess.of("회원 탈퇴가 완료되었습니다. 데이터는 보존되며 필요시 복구 가능합니다."));
    }

    // ==================== 유틸리티 ====================

    private String getCurrentUserEmail() {
        return currentUserFacade.requireCurrentUserEmail();
    }

    /** 서비스 계층이 DB PK 를 기대하므로, 본인 확인 뒤에는 항상 PK 로 정규화해 넘긴다. */
    private String selfDbId() {
        return String.valueOf(currentUserFacade.requireCurrentUserDbId());
    }

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

        String message = isComplete
                ? "프로필이 완성되었습니다!"
                : String.format("프로필 완성도: %d%% (%d개 항목 추가 필요)", percentage, totalFields - completedFields);

        return UserProfileCompletionResponse.builder()
                .isComplete(isComplete)
                .completionPercentage(percentage)
                .message(message)
                .missingFields(missingFields)
                .build();
    }

    private boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }
}
