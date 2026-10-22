package com.carecode.domain.admin.controller;

import com.carecode.domain.admin.dto.AdminNotificationCreateRequest;
import com.carecode.domain.admin.service.AdminNotificationService;
import com.carecode.domain.notification.dto.response.NotificationInfoResponse;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 어드민 알림 관리 API. 조회·발송·삭제는 {@link AdminNotificationService} 가 한다. */
@RestController
@RequestMapping("/api/admin/notifications")
@RequiredArgsConstructor
@Tag(name = "어드민 - 알림", description = "관리자 전용 알림 관리 API")
public class AdminNotificationController {

    private final AdminNotificationService adminNotificationService;

    @GetMapping
    @Operation(summary = "알림 목록 조회")
    public ResponseEntity<Page<NotificationInfoResponse>> list(
            @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(adminNotificationService.list(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "알림 상세 조회")
    public ResponseEntity<NotificationInfoResponse> detail(@PathVariable Long id) {
        return ResponseEntity.ok(adminNotificationService.detail(id));
    }

    @PostMapping
    @Operation(summary = "알림 발송", description = "특정 사용자에게 알림을 만들고 사용자 설정 채널로 발송")
    public ResponseEntity<NotificationInfoResponse> create(
            @Valid @RequestBody AdminNotificationCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminNotificationService.send(request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "알림 삭제")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        adminNotificationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
