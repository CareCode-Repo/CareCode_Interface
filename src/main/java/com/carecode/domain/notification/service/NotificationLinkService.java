package com.carecode.domain.notification.service;

import com.carecode.core.analytics.EventLogger;
import com.carecode.core.analytics.EventType;
import com.carecode.core.exception.ResourceNotFoundException;
import com.carecode.domain.notification.entity.Notification;
import com.carecode.domain.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 알림을 거쳐 들어온 재방문을 집계한다.
 * 알림이 실제로 사람을 돌아오게 하는지는 이 전환율로만 알 수 있다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationLinkService {

    private final NotificationRepository notificationRepository;
    private final EventLogger eventLogger;

    /**
     * 클릭을 집계하고, 본인 알림이면 읽음으로 표시한다.
     *
     * @param actorUserId 비로그인 클릭도 집계 대상이라 {@code null} 일 수 있다
     * @return 집계한 알림의 유형
     */
    @Transactional
    public Notification.NotificationType recordClick(Long notificationId, Long actorUserId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("알림을 찾을 수 없습니다: " + notificationId));

        // 본인 알림만 열람 표시를 남긴다. 남의 알림 ID 로 읽음 처리되면 안 된다.
        if (actorUserId != null && notification.getUser() != null
                && actorUserId.equals(notification.getUser().getId())) {
            notification.setIsRead(true);
            notificationRepository.save(notification);
        }

        eventLogger.log(EventType.NOTIFICATION_CLICKED, actorUserId,
                String.valueOf(notificationId), notification.getNotificationType().name());

        return notification.getNotificationType();
    }
}
