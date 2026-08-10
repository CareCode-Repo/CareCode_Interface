package com.carecode.domain.notification.controller;

import com.carecode.core.analytics.EventLogger;
import com.carecode.core.analytics.EventType;
import com.carecode.core.exception.CareServiceException;
import com.carecode.core.security.CurrentUserFacade;
import com.carecode.domain.notification.entity.Notification;
import com.carecode.domain.notification.repository.NotificationRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * 알림을 거쳐 들어온 재방문을 집계한다.
 * 알림이 실제로 사람을 돌아오게 하는지는 이 전환율로만 알 수 있다.
 */
@Slf4j
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Tag(name = "알림", description = "알림 API")
public class NotificationLinkController {

    /** 열어줄 수 있는 화면. 임의 경로를 허용하면 오픈 리다이렉트가 된다. */
    private static final Set<String> ALLOWED_TARGETS = Set.of(
            "policies", "policy", "facilities", "facility", "children", "health", "notifications");

    private final NotificationRepository notificationRepository;
    private final EventLogger eventLogger;
    private final CurrentUserFacade currentUserFacade;

    @Value("${app.notification.deep-link-base:carecode://}")
    private String deepLinkBase;

    @GetMapping("/{notificationId}/open")
    @Operation(summary = "알림 열기", description = "클릭을 집계한 뒤 해당 화면으로 이동")
    public ResponseEntity<Void> open(
            @Parameter(description = "알림 ID", required = true) @PathVariable Long notificationId,
            @Parameter(description = "이동할 화면 (policies, facilities 등)") @RequestParam(required = false) String target,
            @Parameter(description = "대상 식별자") @RequestParam(required = false) String targetId) {

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new CareServiceException("알림을 찾을 수 없습니다: " + notificationId));

        Long userId = currentUserIdOrNull();
        // 본인 알림만 열람 표시를 남긴다. 남의 알림 ID 로 읽음 처리되면 안 된다.
        if (userId != null && notification.getUser() != null
                && userId.equals(notification.getUser().getId())) {
            notification.setIsRead(true);
            notificationRepository.save(notification);
        }

        eventLogger.log(EventType.NOTIFICATION_CLICKED, userId,
                String.valueOf(notificationId), notification.getNotificationType().name());

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(buildDeepLink(target, targetId)))
                .build();
    }

    /** 허용 목록에 없는 화면은 홈으로 보낸다. */
    private String buildDeepLink(String target, String targetId) {
        if (target == null || !ALLOWED_TARGETS.contains(target)) {
            return deepLinkBase;
        }
        StringBuilder link = new StringBuilder(deepLinkBase).append(target);
        if (targetId != null && !targetId.isBlank()) {
            link.append('/').append(URI.create("").resolve(encode(targetId)));
        }
        return link.toString();
    }

    private String encode(String value) {
        return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /** 알림 클릭은 비로그인 상태에서도 발생할 수 있어 인증 실패로 막지 않는다. */
    private Long currentUserIdOrNull() {
        try {
            return currentUserFacade.requireCurrentUserDbId();
        } catch (Exception e) {
            return null;
        }
    }
}
