package com.carecode.domain.notification.sender;

import com.carecode.domain.notification.entity.PushDevice;
import com.carecode.domain.notification.repository.PushDeviceRepository;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 푸시 발송.
 *
 * <p>한 사람이 휴대폰과 웹을 함께 쓴다. 한 대만 보내면 나머지는 <b>아무 말 없이</b> 알림을
 * 받지 못하고, 사용자는 "알림이 안 와요" 라고만 말할 수 있다 — 어느 기기인지도, 왜인지도
 * 모른다. 그래서 여기가 조용히 틀리기 가장 쉬운 자리다.
 */
@DisplayName("푸시 발송")
class PushNotificationSenderTest {

    private FirebaseMessaging firebaseMessaging;
    private PushDeviceRepository pushDeviceRepository;
    private PushNotificationSender sender;

    @BeforeEach
    void setUp() {
        firebaseMessaging = mock(FirebaseMessaging.class);
        pushDeviceRepository = mock(PushDeviceRepository.class);
        sender = new PushNotificationSender(firebaseMessaging, pushDeviceRepository);
    }

    private NotificationPayload payload(String... tokens) {
        return NotificationPayload.builder()
                .title("새 지원금이 열렸어요")
                .message("확인해 보세요")
                .deviceTokens(List.of(tokens))
                .build();
    }

    /**
     * FCM 예외는 생성자가 공개돼 있지 않아 목으로 만든다.
     *
     * <p>이 호출을 {@code when(...).thenThrow(...)} 의 인자 안에서 하면 안 된다 — 목을 만드는
     * 도중에 또 스터빙을 시작하는 꼴이라 Mockito 가 UnfinishedStubbingException 을 던진다.
     * 반드시 먼저 만들어 변수에 담아 둔다.
     */
    private FirebaseMessagingException messagingException(MessagingErrorCode code) {
        FirebaseMessagingException e = mock(FirebaseMessagingException.class);
        when(e.getMessagingErrorCode()).thenReturn(code);
        return e;
    }

    @Nested
    @DisplayName("여러 기기")
    class ManyDevices {

        @Test
        @DisplayName("등록된 기기 전부에 보낸다")
        void sendsToEveryDevice() throws Exception {
            assertThat(sender.send(payload("token-phone", "token-web"))).isTrue();

            ArgumentCaptor<Message> sent = ArgumentCaptor.forClass(Message.class);
            verify(firebaseMessaging, times(2)).send(sent.capture());
            assertThat(sent.getAllValues()).hasSize(2);
        }

        @Test
        @DisplayName("한 대가 실패해도 나머지는 계속 보낸다")
        void keepsGoingAfterOneFailure() throws Exception {
            FirebaseMessagingException failure = messagingException(MessagingErrorCode.INTERNAL);
            when(firebaseMessaging.send(any())).thenThrow(failure).thenReturn("ok");

            // 기기 하나가 실패했다고 다른 기기까지 알림을 못 받을 이유가 없다.
            assertThat(sender.send(payload("token-broken", "token-ok"))).isTrue();
            verify(firebaseMessaging, times(2)).send(any());
        }

        @Test
        @DisplayName("전부 실패하면 false")
        void reportsFailureWhenNoneSent() throws Exception {
            FirebaseMessagingException failure = messagingException(MessagingErrorCode.INTERNAL);
            when(firebaseMessaging.send(any())).thenThrow(failure);

            assertThat(sender.send(payload("a", "b"))).isFalse();
        }

        @Test
        @DisplayName("등록된 기기가 없으면 보내지 않는다")
        void skipsWhenNoDevice() throws Exception {
            assertThat(sender.send(payload())).isFalse();

            verify(firebaseMessaging, never()).send(any());
        }
    }

    @Nested
    @DisplayName("죽은 토큰 정리")
    class StaleTokens {

        @Test
        @DisplayName("FCM 이 없는 토큰이라고 하면 지운다")
        void removesUnregisteredToken() throws Exception {
            PushDevice device = PushDevice.builder().token("token-dead").build();
            when(pushDeviceRepository.findByToken("token-dead")).thenReturn(Optional.of(device));
            FirebaseMessagingException dead = messagingException(MessagingErrorCode.UNREGISTERED);
            when(firebaseMessaging.send(any())).thenThrow(dead);

            sender.send(payload("token-dead"));

            // 두면 그 사용자에게 알림을 보낼 때마다 헛호출이 쌓이고 로그가 실패로 가득 찬다.
            verify(pushDeviceRepository).delete(device);
        }

        @Test
        @DisplayName("일시적 실패로는 지우지 않는다")
        void keepsTokenOnTemporaryFailure() throws Exception {
            FirebaseMessagingException temporary = messagingException(MessagingErrorCode.UNAVAILABLE);
            when(firebaseMessaging.send(any())).thenThrow(temporary);

            sender.send(payload("token-ok"));

            // FCM 이 잠깐 죽은 것뿐인데 지우면 그 기기는 영영 알림을 못 받는다.
            verify(pushDeviceRepository, never()).delete(any());
        }
    }

    @Test
    @DisplayName("FCM 설정이 없으면 아무것도 하지 않는다")
    void inactiveWithoutFirebase() throws Exception {
        PushNotificationSender disabled = new PushNotificationSender(null, pushDeviceRepository);

        assertThat(disabled.isAvailable()).isFalse();
        assertThat(disabled.send(payload("token"))).isFalse();
    }
}
