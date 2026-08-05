package com.carecode.domain.admin.controller;

import com.carecode.core.exception.ResourceNotFoundException;
import com.carecode.core.exception.UserNotFoundException;
import com.carecode.domain.admin.dto.AdminNotificationCreateRequest;
import com.carecode.domain.notification.dto.response.NotificationInfoResponse;
import com.carecode.domain.notification.entity.Notification;
import com.carecode.domain.notification.repository.NotificationRepository;
import com.carecode.domain.user.entity.User;
import com.carecode.domain.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/** 어드민 알림 관리 API. */
@RestController
@RequestMapping("/api/admin/notifications")
@RequiredArgsConstructor
@Tag(name = "어드민 - 알림", description = "관리자 전용 알림 관리 API")
public class AdminNotificationController {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @GetMapping
    @Operation(summary = "알림 목록 조회")
    public ResponseEntity<Page<NotificationInfoResponse>> list(
            @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(notificationRepository.findAll(pageable).map(this::toResponse));
    }

    @GetMapping("/{id}")
    @Operation(summary = "알림 상세 조회")
    public ResponseEntity<NotificationInfoResponse> detail(@PathVariable Long id) {
        return ResponseEntity.ok(toResponse(findNotification(id)));
    }

    @PostMapping
    @Operation(summary = "알림 발송", description = "특정 사용자에게 알림 생성")
    @Transactional
    public ResponseEntity<NotificationInfoResponse> create(
            @Valid @RequestBody AdminNotificationCreateRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다: " + request.getUserId()));

        Notification notification = Notification.builder()
                .user(user)
                .notificationType(request.getNotificationType())
                .title(request.getTitle())
                .message(request.getMessage())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toResponse(notificationRepository.save(notification)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "알림 삭제")
    @Transactional
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        notificationRepository.delete(findNotification(id));
        return ResponseEntity.noContent().build();
    }

    private Notification findNotification(Long id) {
        return notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("알림을 찾을 수 없습니다. ID: " + id));
    }

    private NotificationInfoResponse toResponse(Notification notification) {
        return NotificationInfoResponse.builder()
                .id(notification.getId())
                .userId(notification.getUser() != null ? notification.getUser().getUserId() : null)
                .notificationType(notification.getNotificationType() != null
                        ? notification.getNotificationType().name() : null)
                .title(notification.getTitle())
                .message(notification.getMessage())
                .isRead(Boolean.TRUE.equals(notification.getIsRead()))
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
