package com.carecode.domain.notification.sender;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

/**
 * FCM 푸시 채널 발송기.
 *
 * <p>{@link FirebaseMessaging} 빈이 없으면(자격증명 미설정) 비활성 상태로 동작한다.
 */
@Slf4j
@Component
public class PushNotificationSender implements NotificationSender {

    private final FirebaseMessaging firebaseMessaging;

    public PushNotificationSender(@Autowired(required = false) @Nullable FirebaseMessaging firebaseMessaging) {
        this.firebaseMessaging = firebaseMessaging;
    }

    @Override
    public NotificationChannelType channel() {
        return NotificationChannelType.PUSH;
    }

    @Override
    public boolean isAvailable() {
        return firebaseMessaging != null;
    }

    @Override
    public boolean send(NotificationPayload payload) {
        if (!isAvailable()) {
            log.debug("푸시 발송 건너뜀 - FCM 미설정");
            return false;
        }

        String token = payload.getDeviceToken();
        if (token == null || token.isBlank()) {
            log.debug("푸시 발송 건너뜀 - 디바이스 토큰 없음");
            return false;
        }

        try {
            Message message = Message.builder()
                    .setToken(token)
                    .setNotification(com.google.firebase.messaging.Notification.builder()
                            .setTitle(payload.getTitle())
                            .setBody(payload.getMessage())
                            .build())
                    .build();

            firebaseMessaging.send(message);
            return true;
        } catch (Exception e) {
            log.error("푸시 발송 실패 - token={}...", token.length() > 8 ? token.substring(0, 8) : token, e);
            return false;
        }
    }
}
