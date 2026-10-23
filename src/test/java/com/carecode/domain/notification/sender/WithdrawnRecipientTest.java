package com.carecode.domain.notification.sender;

import com.carecode.domain.notification.entity.Notification;
import com.carecode.domain.notification.repository.NotificationPreferenceRepository;
import com.carecode.domain.notification.repository.PushDeviceRepository;
import com.carecode.domain.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 탈퇴·비활성 계정에는 외부 발송을 하지 않는다.
 *
 * <p>탈퇴는 물리 삭제가 아니다({@link User#withdraw()}) — 행이 남고 {@code isActive=false},
 * {@code deletedAt} 이 찍히며 이메일은 {@code deleted_<id>@deleted.local} 로 익명화된다.
 * 그런데 사용자가 남긴 <b>대기 등록·알림 설정·푸시 토큰은 그대로 남습니다.</b>
 *
 * <p>그래서 이런 일이 생겼다. 어린이집 대기를 걸어 둔 사람이 탈퇴한다 → 대기 기록은 WAITING 으로
 * 남아 있다 → 그 시설에 자리가 난다 → {@code FacilityVacancyNotifier} 가
 * {@code entry.getUser()} 로 알림을 만들고 발송한다. 디스패처는 수신자가 {@code null} 인지만
 * 봤으므로 <b>탈퇴한 계정의 기기로 푸시가 가고, 익명화된 주소로 메일이 나간다.</b>
 *
 * <p>발송 경로는 넷이지만(리마인더 배치, 빈자리·마감 알림, 사용자 API, 어드민) 모두 이
 * 디스패처를 거친다. 그래서 막는 자리도 여기 하나다.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("탈퇴 계정 발송 차단")
class WithdrawnRecipientTest {

    @Mock private NotificationPreferenceRepository preferenceRepository;
    @Mock private PushDeviceRepository pushDeviceRepository;

    /** 호출 여부만 기록한다. 실제 발송은 하지 않는다. */
    private static final class RecordingSender implements NotificationSender {
        private final List<NotificationPayload> sent = new ArrayList<>();

        @Override public NotificationChannelType channel() {
            return NotificationChannelType.EMAIL;
        }

        @Override public boolean isAvailable() {
            return true;
        }

        @Override public String getUnavailableReason() {
            return null;
        }

        @Override public boolean send(NotificationPayload payload) {
            sent.add(payload);
            return true;
        }
    }

    private Notification notificationFor(User recipient) {
        return Notification.builder()
                .id(1L)
                .user(recipient)
                .notificationType(Notification.NotificationType.FACILITY)
                .title("자리가 났습니다")
                .message("확인해 보세요")
                .build();
    }

    private User activeUser() {
        return User.builder().id(1L).userId("user_1").email("a@example.com")
                .name("보호자").isActive(true).build();
    }

    @Test
    @DisplayName("탈퇴한 계정에는 발송하지 않는다")
    void withdrawnUserGetsNothing() {
        User user = activeUser();
        user.withdraw();   // isActive=false, deletedAt 설정, 이메일 익명화

        RecordingSender sender = new RecordingSender();
        NotificationDispatcher dispatcher = new NotificationDispatcher(
                List.of(sender), preferenceRepository, pushDeviceRepository);

        boolean sent = dispatcher.dispatch(notificationFor(user));

        assertThat(sent).as("탈퇴 계정 발송 결과").isFalse();
        assertThat(sender.sent).as("탈퇴 계정으로 나간 발송").isEmpty();
    }

    /** 관리자가 비활성화한 계정도 같다. 로그인할 수 없는 계정에 알림을 보낼 이유가 없다. */
    @Test
    @DisplayName("비활성 계정에도 발송하지 않는다")
    void inactiveUserGetsNothing() {
        User user = activeUser();
        user.setIsActive(false);

        RecordingSender sender = new RecordingSender();
        NotificationDispatcher dispatcher = new NotificationDispatcher(
                List.of(sender), preferenceRepository, pushDeviceRepository);

        assertThat(dispatcher.dispatch(notificationFor(user))).isFalse();
        assertThat(sender.sent).isEmpty();
    }

    /**
     * 차단이 과하면 아무에게도 알림이 가지 않는다. 멀쩡한 계정은 통과해야 한다.
     *
     * <p>설정이 없을 때 어떤 채널이 열리는지는 {@code NotificationDispatcher} 의 기본값이
     * 정하므로, 여기서는 "탈퇴 차단에 걸리지 않는다" 까지만 본다.
     */
    @Test
    @DisplayName("정상 계정은 차단되지 않는다")
    void activeUserIsNotBlocked() {
        User user = activeUser();

        NotificationDispatcher dispatcher = new NotificationDispatcher(
                List.of(new RecordingSender()), preferenceRepository, pushDeviceRepository);

        // 인앱은 알림 레코드 자체가 전달 수단이라 설정이 없어도 전달로 본다.
        assertThat(dispatcher.dispatch(notificationFor(user)))
                .as("정상 계정은 적어도 인앱으로 전달된다")
                .isTrue();
    }
}
