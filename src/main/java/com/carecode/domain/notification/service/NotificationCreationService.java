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

    /**
     * 알림을 저장하고 사용자 설정 채널로 <b>동기</b> 발송한다. 배치에서 쓴다.
     *
     * <p>배치는 발송 결과를 즉시 알아야 한다(몇 건 나갔는지 집계해 돌려준다). 요청을 처리하는
     * 중이라면 SMTP·FCM 왕복이 응답을 막으므로 {@link #createAndSendAsync} 를 쓴다.
     */
    @Transactional
    public Notification createAndSend(User recipient,
                                      Notification.NotificationType type,
                                      String title,
                                      String message) {
        Notification saved = save(recipient, type, title, message);

        boolean sent = notificationDispatcher.dispatch(saved);
        if (!sent) {
            log.debug("알림 외부 채널 발송 없음(인앱만) - notificationId={}", saved.getId());
        }
        return saved;
    }

    /**
     * 알림을 저장하고 발송은 비동기로 넘긴다. 요청을 처리하는 중(어드민 발송 등)에 쓴다.
     *
     * <p>저장만 하고 끝내면 앱 알림함에만 쌓이고 푸시·메일이 나가지 않는다. 보낸 사람은
     * 201 을 받으므로 나갔다고 믿는다 — 어드민 "알림 발송" 이 실제로 그 상태였다.
     */
    @Transactional
    public Notification createAndSendAsync(User recipient,
                                           Notification.NotificationType type,
                                           String title,
                                           String message) {
        Notification saved = save(recipient, type, title, message);
        notificationDispatcher.dispatchAsync(saved);
        return saved;
    }

    private Notification save(User recipient,
                              Notification.NotificationType type,
                              String title,
                              String message) {
        return notificationRepository.save(Notification.builder()
                .user(recipient)
                .notificationType(type)
                .title(title)
                .message(message)
                .isRead(false)
                .build());
    }
}
