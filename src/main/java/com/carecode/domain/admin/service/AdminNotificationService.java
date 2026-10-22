package com.carecode.domain.admin.service;

import com.carecode.core.exception.ResourceNotFoundException;
import com.carecode.core.exception.UserNotFoundException;
import com.carecode.domain.admin.dto.AdminNotificationCreateRequest;
import com.carecode.domain.notification.dto.response.NotificationInfoResponse;
import com.carecode.domain.notification.entity.Notification;
import com.carecode.domain.notification.repository.NotificationRepository;
import com.carecode.domain.notification.service.NotificationCreationService;
import com.carecode.domain.user.entity.User;
import com.carecode.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 어드민 알림 관리.
 *
 * <p>컨트롤러가 직접 리포지토리를 쓰고 있어서 생긴 문제를 여기로 모았다. 발송이 그 예다 —
 * 알림을 만드는 경로가 넷이었고 셋은 저장 후 발송까지 하는데, <b>어드민 경로만 저장에서
 * 끝났다.</b> 관리자가 보낸 알림은 앱 알림함에만 쌓이고 푸시·메일이 나가지 않았다.
 * 응답은 201 이라 보낸 사람은 나갔다고 믿는다.
 *
 * <p>이제 생성은 {@link NotificationCreationService} 한 곳을 거친다. 발송 방식이 바뀌면
 * 네 경로가 함께 바뀐다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminNotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final NotificationCreationService notificationCreationService;

    public Page<NotificationInfoResponse> list(Pageable pageable) {
        return notificationRepository.findAll(pageable).map(AdminNotificationService::toResponse);
    }

    public NotificationInfoResponse detail(Long id) {
        return toResponse(require(id));
    }

    /**
     * 특정 사용자에게 알림을 발송한다.
     *
     * <p>발송은 비동기다. 관리자가 응답을 기다리는 동안 SMTP·FCM 왕복을 하면 요청이 막힌다.
     */
    @Transactional
    public NotificationInfoResponse send(AdminNotificationCreateRequest request) {
        User recipient = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new UserNotFoundException(
                        "사용자를 찾을 수 없습니다: " + request.getUserId()));

        return toResponse(notificationCreationService.createAndSendAsync(
                recipient, request.getNotificationType(), request.getTitle(), request.getMessage()));
    }

    @Transactional
    public void delete(Long id) {
        notificationRepository.delete(require(id));
    }

    private Notification require(Long id) {
        return notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("알림을 찾을 수 없습니다. ID: " + id));
    }

    private static NotificationInfoResponse toResponse(Notification notification) {
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
