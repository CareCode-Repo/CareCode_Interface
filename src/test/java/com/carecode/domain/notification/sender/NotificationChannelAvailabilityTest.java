package com.carecode.domain.notification.sender;

import com.carecode.domain.notification.entity.Notification;
import com.carecode.domain.notification.entity.NotificationPreference;
import com.carecode.domain.notification.repository.NotificationPreferenceRepository;
import com.carecode.domain.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * 채널 가용 여부 검증.
 *
 * <p>설정 화면은 이 값을 보고 "켜도 오지 않는" 채널을 잠근다. {@code dispatch} 가 채널을 건너뛰는
 * 조건과 어긋나면, 사용자는 켜 둔 채로 오지 않는 알림을 기다리게 된다.
 */
@ExtendWith(MockitoExtension.class)
class NotificationChannelAvailabilityTest {

    @Mock
    private NotificationPreferenceRepository preferenceRepository;

    /** 테스트용 발송기. 실제 발송은 하지 않는다. */
    private record StubSender(NotificationChannelType channel, boolean available, String reason)
            implements NotificationSender {

        @Override
        public NotificationChannelType channel() {
            return channel;
        }

        @Override
        public boolean isAvailable() {
            return available;
        }

        @Override
        public String getUnavailableReason() {
            return reason;
        }

        @Override
        public boolean send(NotificationPayload payload) {
            return false;
        }
    }

    private NotificationDispatcher dispatcherWith(NotificationSender... senders) {
        return new NotificationDispatcher(List.of(senders), preferenceRepository);
    }

    @Test
    @DisplayName("인앱은 발송기가 없어도 항상 사용할 수 있다 - 알림 레코드 자체가 전달 수단이다")
    void 인앱은_항상_사용할_수_있다() {
        NotificationDispatcher dispatcher = dispatcherWith();

        assertThat(dispatcher.isChannelAvailable(NotificationChannelType.IN_APP)).isTrue();
        assertThat(dispatcher.unavailableReason(NotificationChannelType.IN_APP)).isEmpty();
    }

    @Test
    @DisplayName("발송기가 등록되지 않은 채널은 쓸 수 없고 이유가 비어 있지 않다")
    void 발송기가_없으면_사용할_수_없다() {
        NotificationDispatcher dispatcher = dispatcherWith();

        assertThat(dispatcher.isChannelAvailable(NotificationChannelType.EMAIL)).isFalse();
        assertThat(dispatcher.unavailableReason(NotificationChannelType.EMAIL))
                .isPresent()
                .get()
                .asString()
                .isNotBlank();
    }

    @Test
    @DisplayName("발송기가 비활성이면 발송기가 밝힌 이유를 그대로 전한다")
    void 비활성_발송기의_이유를_그대로_전한다() {
        NotificationDispatcher dispatcher = dispatcherWith(
                new StubSender(NotificationChannelType.SMS, false, "문자 발송은 아직 준비 중이에요."));

        assertThat(dispatcher.isChannelAvailable(NotificationChannelType.SMS)).isFalse();
        assertThat(dispatcher.unavailableReason(NotificationChannelType.SMS))
                .contains("문자 발송은 아직 준비 중이에요.");
    }

    @Test
    @DisplayName("사용 가능한 채널은 이유를 남기지 않는다")
    void 사용_가능하면_이유가_없다() {
        NotificationDispatcher dispatcher = dispatcherWith(
                new StubSender(NotificationChannelType.PUSH, true, null));

        assertThat(dispatcher.isChannelAvailable(NotificationChannelType.PUSH)).isTrue();
        assertThat(dispatcher.unavailableReason(NotificationChannelType.PUSH)).isEmpty();
    }

    @Test
    @DisplayName("푸시 토큰은 알림 유형과 무관하게 찾는다 - 등록은 SYSTEM 행에만 쓰기 때문")
    void 다른_유형에_등록된_토큰으로도_푸시를_보낸다() {
        User recipient = User.builder().id(1L).userId("u-1").name("보호자").build();
        // POLICY 설정 행에는 토큰이 없다. 등록 시 SYSTEM 행에만 저장하기 때문이다.
        NotificationPreference policyPreference = NotificationPreference.builder()
                .user(recipient)
                .notificationType(Notification.NotificationType.POLICY)
                .pushEnabled(true)
                .build();

        when(preferenceRepository.findByUserAndNotificationType(recipient, Notification.NotificationType.POLICY))
                .thenReturn(Optional.of(policyPreference));
        when(preferenceRepository.findDeviceTokensByUser(recipient)).thenReturn(List.of("token-1"));

        RecordingSender push = new RecordingSender(NotificationChannelType.PUSH);
        NotificationDispatcher dispatcher = dispatcherWith(push);

        Notification notification = Notification.builder()
                .user(recipient)
                .notificationType(Notification.NotificationType.POLICY)
                .title("아동수당 신청 마감")
                .message("3일 남았어요")
                .build();

        dispatcher.dispatch(notification);

        assertThat(push.lastPayload).isNotNull();
        assertThat(push.lastPayload.getDeviceToken()).isEqualTo("token-1");
    }

    /** 전달받은 발송 요청을 기록만 하는 발송기. */
    private static final class RecordingSender implements NotificationSender {
        private final NotificationChannelType channel;
        private NotificationPayload lastPayload;

        private RecordingSender(NotificationChannelType channel) {
            this.channel = channel;
        }

        @Override
        public NotificationChannelType channel() {
            return channel;
        }

        @Override
        public boolean send(NotificationPayload payload) {
            this.lastPayload = payload;
            return true;
        }
    }

    @Test
    @DisplayName("채널 키는 설정 변경 API 가 받는 값과 같다")
    void 채널_키가_설정_API_와_일치한다() {
        // 클라이언트가 이 값을 그대로 `.../channels/{channel}` 로 되돌려 보낸다.
        assertThat(NotificationChannelType.IN_APP.getKey()).isEqualTo("inapp");
        assertThat(NotificationChannelType.EMAIL.getKey()).isEqualTo("email");
        assertThat(NotificationChannelType.PUSH.getKey()).isEqualTo("push");
        assertThat(NotificationChannelType.SMS.getKey()).isEqualTo("sms");
    }
}
