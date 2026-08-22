package com.carecode.domain.admin.controller;

import com.carecode.core.exception.UserNotFoundException;
import com.carecode.core.handler.ApiSuccess;
import com.carecode.domain.admin.dto.AdminUserResponse;
import com.carecode.domain.admin.dto.AdminUserUpdateRequest;
import com.carecode.domain.user.dto.response.UserDto;
import com.carecode.domain.user.dto.response.UserStatsResponse;
import com.carecode.domain.user.entity.User;
import com.carecode.domain.user.repository.UserRepository;
import com.carecode.domain.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 어드민 사용자 관리 API.
 *
 * <p>접근 제어를 이중으로 건다. SecurityConfig 의 {@code /api/admin/**} → {@code hasRole("ADMIN")} URL 규칙과,
 * 클래스 레벨 {@code @PreAuthorize}. URL 규칙 하나에만 의존하면 경로가 바뀌거나 앞선 매처가
 * 이 경로를 삼켰을 때 조용히 열린다. 실제로 이 프로젝트는 {@code /health/**} 가
 * {@code /hospitals/**} 를 삼켜 공개 API 가 통째로 막힌 전례가 있다.
 *
 * <p>여기 있는 조회·변경 기능은 원래 {@code /users} 에 있었다. 그쪽은 클래스 제약이
 * {@code isAuthenticated()} 뿐이어서, 아무 회원이나 남의 역할을 ADMIN 으로 바꾸거나
 * 전체 회원 개인정보를 열람할 수 있었다.
 */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "어드민 - 사용자", description = "관리자 전용 사용자 관리 API")
public class AdminUserController {

    private final UserRepository userRepository;
    private final UserService userService;

    // ==================== 조회 ====================

    @GetMapping
    @Operation(summary = "사용자 목록 조회")
    public ResponseEntity<Page<AdminUserResponse>> list(
            @PageableDefault(size = 50, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(userRepository.findAll(pageable).map(AdminUserResponse::from));
    }

    @GetMapping("/statistics")
    @Operation(summary = "사용자 통계 조회")
    public ResponseEntity<UserStatsResponse> statistics() {
        return ResponseEntity.ok(userService.getUserStatistics());
    }

    @GetMapping("/search")
    @Operation(summary = "사용자 검색", description = "이름 또는 이메일 키워드로 검색")
    public ResponseEntity<List<UserDto>> search(@RequestParam String keyword,
                                                @RequestParam(required = false) String type) {
        List<UserDto> users = (type != null && !type.isEmpty())
                ? userService.searchUsers(keyword, type)
                : userService.searchUsers(keyword);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/active")
    @Operation(summary = "활성 사용자 목록")
    public ResponseEntity<List<UserDto>> activeUsers() {
        return ResponseEntity.ok(userService.getActiveUsers());
    }

    @GetMapping("/verified")
    @Operation(summary = "이메일 인증 완료 사용자 목록")
    public ResponseEntity<List<UserDto>> verifiedUsers() {
        return ResponseEntity.ok(userService.getVerifiedUsers());
    }

    @GetMapping("/recently-active")
    @Operation(summary = "최근 활동 사용자 목록")
    public ResponseEntity<List<UserDto>> recentlyActiveUsers() {
        return ResponseEntity.ok(userService.getRecentlyActiveUsers());
    }

    @GetMapping("/by-type/{userType}")
    @Operation(summary = "사용자 유형별 조회")
    public ResponseEntity<List<UserDto>> usersByType(@PathVariable String userType) {
        return ResponseEntity.ok(userService.getUsersByType(userType));
    }

    @GetMapping("/by-region/{region}")
    @Operation(summary = "지역별 사용자 조회")
    public ResponseEntity<List<UserDto>> usersByRegion(@PathVariable String region) {
        return ResponseEntity.ok(userService.getUsersByRegion(region));
    }

    @GetMapping("/{id}")
    @Operation(summary = "사용자 상세 조회")
    public ResponseEntity<AdminUserResponse> detail(@PathVariable Long id) {
        return ResponseEntity.ok(AdminUserResponse.from(findUser(id)));
    }

    // ==================== 변경 ====================

    @PatchMapping("/{id}")
    @Operation(summary = "사용자 정보 수정", description = "이름·연락처·역할·활성 상태만 변경할 수 있습니다")
    @Transactional
    public ResponseEntity<AdminUserResponse> update(@PathVariable Long id,
                                                    @Valid @RequestBody AdminUserUpdateRequest request) {
        User user = findUser(id);

        if (request.getName() != null) {
            user.setName(request.getName());
        }
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }
        if (request.getIsActive() != null) {
            user.setIsActive(request.getIsActive());
        }
        user.setUpdatedAt(LocalDateTime.now());

        return ResponseEntity.ok(AdminUserResponse.from(userRepository.save(user)));
    }

    @PutMapping("/{id}/role")
    @Operation(summary = "사용자 역할 변경", description = "PARENT / ADMIN 등 역할을 변경합니다")
    public ResponseEntity<ApiSuccess> updateRole(@PathVariable Long id,
                                                 @RequestBody Map<String, String> request) {
        String newRole = request.get("role");
        if (newRole == null || newRole.trim().isEmpty()) {
            throw new IllegalArgumentException("역할은 필수입니다");
        }
        userService.updateUserRole(id, newRole.trim());
        return ResponseEntity.ok(ApiSuccess.of("사용자 역할이 변경되었습니다"));
    }

    @PutMapping("/{id}/activate")
    @Operation(summary = "사용자 활성화", description = "비활성화된 사용자를 다시 활성화합니다")
    public ResponseEntity<ApiSuccess> activate(@PathVariable Long id) {
        userService.activateUser(String.valueOf(id));
        return ResponseEntity.ok(ApiSuccess.of("사용자가 활성화되었습니다"));
    }

    @PutMapping("/{id}/reactivate")
    @Operation(summary = "탈퇴 계정 복구",
            description = "소프트 삭제된 계정을 복구합니다. 탈퇴한 본인은 로그인할 수 없으므로 관리자만 수행할 수 있습니다")
    public ResponseEntity<ApiSuccess> reactivate(@PathVariable Long id) {
        userService.reactivateUser(String.valueOf(id));
        return ResponseEntity.ok(ApiSuccess.of("계정이 복구되었습니다"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "사용자 탈퇴 처리", description = "물리 삭제 대신 soft delete 로 비활성화")
    @Transactional
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        User user = findUser(id);
        // 하드 삭제하면 게시글·건강기록 등 참조 데이터가 함께 사라지므로 soft delete 로 처리한다.
        user.setDeletedAt(LocalDateTime.now());
        user.setIsActive(false);
        userRepository.save(user);
        return ResponseEntity.noContent().build();
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다: " + id));
    }
}
