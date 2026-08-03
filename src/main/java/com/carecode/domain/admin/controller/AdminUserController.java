package com.carecode.domain.admin.controller;

import com.carecode.core.exception.UserNotFoundException;
import com.carecode.domain.admin.dto.AdminUserResponse;
import com.carecode.domain.admin.dto.AdminUserUpdateRequest;
import com.carecode.domain.user.entity.User;
import com.carecode.domain.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 어드민 사용자 관리 API.
 *
 * <p>접근 제어는 SecurityConfig 의 {@code /api/admin/**} → hasRole("ADMIN") 규칙이 담당한다.
 */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@Tag(name = "어드민 - 사용자", description = "관리자 전용 사용자 관리 API")
public class AdminUserController {

    private final UserRepository userRepository;

    @GetMapping
    @Operation(summary = "사용자 목록 조회")
    public ResponseEntity<Page<AdminUserResponse>> list(
            @PageableDefault(size = 50, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(userRepository.findAll(pageable).map(AdminUserResponse::from));
    }

    @GetMapping("/{id}")
    @Operation(summary = "사용자 상세 조회")
    public ResponseEntity<AdminUserResponse> detail(@PathVariable Long id) {
        return ResponseEntity.ok(AdminUserResponse.from(findUser(id)));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "사용자 정보 수정", description = "이름·연락처·역할·활성 상태만 변경할 수 있습니다.")
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

    @DeleteMapping("/{id}")
    @Operation(summary = "사용자 탈퇴 처리", description = "물리 삭제 대신 soft delete 로 비활성화합니다.")
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
