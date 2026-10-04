package com.carecode.domain.notification.sender;

import com.carecode.domain.notification.repository.PushDeviceRepository;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

/** FCM 푸시 채널 발송기. FirebaseMessaging 빈이 없으면(자격증명 미설정) 비활성 상태로 동작한다. */
@Slf4j
@Component
public class PushNotificationSender implements NotificationSender {

    private final FirebaseMessaging firebaseMessaging;
    private final PushDeviceRepository pushDeviceRepository;

    public PushNotificationSender(@Autowired(required = false) @Nullable FirebaseMessaging firebaseMessaging,
                                  PushDeviceRepository pushDeviceRepository) {
        this.firebaseMessaging = firebaseMessaging;
        this.pushDeviceRepository = pushDeviceRepository;
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
    public String getUnavailableReason() {
        return isAvailable() ? null : "푸시 발송이 아직 설정되지 않았어요.";
    }

    /**
     * 사용자의 모든 기기로 보낸다.
     *
     * <p>한 사람이 휴대폰과 웹을 함께 쓴다. 한 대만 보내면 나머지는 아무 말 없이 알림을 받지
     * 못하고, 사용자는 "알림이 안 와요" 라고만 말할 수 있다.
     *
     * <p>한 대가 실패해도 나머지는 계속 보낸다. 기기 하나가 앱을 지웠다고 다른 기기까지
     * 알림을 못 받을 이유가 없다.
     *
     * @return 한 대라도 보냈으면 true
     */
    @Override
    public boolean send(NotificationPayload payload) {
        if (!isAvailable()) {
            log.debug("푸시 발송 건너뜀 - FCM 미설정");
            return false;
        }

        List<String> tokens = payload.resolveDeviceTokens();
        if (tokens.isEmpty()) {
            log.debug("푸시 발송 건너뜀 - 등록된 기기 없음");
            return false;
        }

        boolean anySent = false;
        for (String token : tokens) {
            anySent |= sendToToken(token, payload);
        }
        return anySent;
    }

    private boolean sendToToken(String token, NotificationPayload payload) {
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
        } catch (FirebaseMessagingException e) {
            if (isDeadToken(e)) {
                forgetToken(token);
            } else {
                log.error("푸시 발송 실패 - token={}...", mask(token), e);
            }
            return false;
        } catch (Exception e) {
            log.error("푸시 발송 실패 - token={}...", mask(token), e);
            return false;
        }
    }

    /**
     * FCM 이 "이 토큰은 더 이상 없다" 고 답했는지.
     *
     * <p>앱을 지웠거나 데이터를 초기화하면 토큰이 죽는다. 그대로 두면 그 사용자에게 알림을
     * 보낼 때마다 헛호출이 쌓이고, 로그는 실패로 가득 차 진짜 문제를 덮는다.
     */
    private boolean isDeadToken(FirebaseMessagingException e) {
        MessagingErrorCode code = e.getMessagingErrorCode();
        return code == MessagingErrorCode.UNREGISTERED || code == MessagingErrorCode.INVALID_ARGUMENT;
    }

    /**
     * 죽은 토큰을 지운다.
     *
     * <p>{@code delete(entity)} 를 쓰는 이유: 파생 삭제 질의({@code deleteByToken})는 호출하는
     * 쪽에 트랜잭션이 있어야 한다. 발송은 비동기이고 네트워크 호출 중이라 트랜잭션을 열어 두고
     * 싶지 않다. {@code JpaRepository.delete} 는 그 자체로 트랜잭션 안에서 돈다.
     */
    private void forgetToken(String token) {
        try {
            pushDeviceRepository.findByToken(token).ifPresent(pushDeviceRepository::delete);
            log.info("더 이상 쓰이지 않는 푸시 토큰 제거 - token={}...", mask(token));
        } catch (Exception e) {
            log.warn("푸시 토큰 제거 실패 - token={}...", mask(token), e);
        }
    }

    /** 토큰은 그 자체가 발송 권한이다. 로그에 전문을 남기지 않는다. */
    private String mask(String token) {
        return token != null && token.length() > 8 ? token.substring(0, 8) : token;
    }
}
