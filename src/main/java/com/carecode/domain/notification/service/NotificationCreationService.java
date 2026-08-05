package com.carecode.domain.notification.service;

import com.carecode.domain.notification.entity.Notification;
import com.carecode.domain.notification.repository.NotificationRepository;
import com.carecode.domain.notification.sender.NotificationDispatcher;
import com.carecode.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 시스템이 발생시키는 알림 생성 경로. NotificationService#createNotification 은 "본인이 본인에게" 만드는 사용자 API 라 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationCreationService {

    private final NotificationRepository notificationRepository;
    private final NotificationDispatcher notificationDispatcher;

    /** 알림을 저장하고 사용자 설정 채널로 발송한다. */
    @Transactional
    public Notification createAndSend(User recipient,
                                      Notification.NotificationType type,
                                      String title,
                                      String message) {
        Notification notification = Notification.builder()
                .user(recipient)
                .notificationType(type)
                .title(title)
                .message(message)
                .isRead(false)
                .build();

        Notification saved = notificationRepository.save(notification);

        // 배치에서는 발송 결과를 즉시 알 수 있도록 동기 발송한다.
        boolean sent = notificationDispatcher.dispatch(saved);
        if (!sent) {
            log.debug("알림 외부 채널 발송 없음(인앱만) - notificationId={}", saved.getId());
        }
        return saved;
    }
}
