package com.carecode.domain.notification.service;

import com.carecode.domain.notification.dto.response.NotificationSettingsResponse;
import com.carecode.domain.notification.entity.Notification;
import com.carecode.domain.notification.entity.NotificationPreference;
import com.carecode.domain.notification.repository.NotificationPreferenceRepository;
import com.carecode.domain.notification.sender.NotificationChannelType;
import com.carecode.domain.user.entity.User;
import com.carecode.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 알림 설정 기본값 검증.
 *
 * <p>설정 행이 없는 사용자에게 {@code NotificationDispatcher} 는 인앱과 푸시만 발송한다.
 * 설정 화면에서 토글 하나를 건드리면 그 시점에 기본 행이 만들어지므로, 기본 행의 채널 구성이
 * 위 규칙과 어긋나면 사용자가 요청한 적 없는 채널이 켜진다. 두 값이 함께 움직이도록 고정한다.
 */
@ExtendWith(MockitoExtension.class)
class NotificationPreferenceDefaultsTest {

    private static final String USER_ID = "u-1";

    @Mock
    private NotificationPreferenceRepository preferenceRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NotificationPreferenceService preferenceService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L).userId(USER_ID).email("parent@example.com").name("보호자")
                .build();
    }

    @Test
    @DisplayName("설정이 없으면 기본값은 인앱·푸시만 켠다 - 이메일과 SMS 는 사용자가 직접 켜야 한다")
    void 설정이_없으면_인앱과_푸시만_켠다() {
        when(userRepository.findByUserId(USER_ID)).thenReturn(Optional.of(user));
        when(preferenceRepository.findByUserAndNotificationType(user, Notification.NotificationType.POLICY))
                .thenReturn(Optional.empty());
        when(preferenceRepository.save(any(NotificationPreference.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        NotificationSettingsResponse settings =
                preferenceService.getPreferenceByType(USER_ID, Notification.NotificationType.POLICY);

        assertThat(settings.getInAppEnabled()).isTrue();
        assertThat(settings.getPushEnabled()).isTrue();
        assertThat(settings.getEmailEnabled()).isFalse();
        assertThat(settings.getSmsEnabled()).isFalse();
    }

    @Test
    @DisplayName("모두 끄기는 설정 행이 없는 유형까지 끈다 - 행이 없으면 기본값으로 계속 발송되기 때문")
    void 모두_끄기가_저장되지_않은_유형까지_끈다() {
        when(userRepository.findByUserId(USER_ID)).thenReturn(Optional.of(user));
        when(preferenceRepository.findByUserOrderByNotificationType(user)).thenReturn(List.of());
        when(preferenceRepository.save(any(NotificationPreference.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        preferenceService.disableAllNotifications(USER_ID);

        ArgumentCaptor<NotificationPreference> captor = ArgumentCaptor.forClass(NotificationPreference.class);
        verify(preferenceRepository, atLeastOnce()).save(captor.capture());

        assertThat(captor.getAllValues())
                .extracting(NotificationPreference::getNotificationType)
                .containsAll(Arrays.asList(Notification.NotificationType.values()));
        assertThat(captor.getAllValues()).allSatisfy(preference -> {
            assertThat(preference.getInAppEnabled()).isFalse();
            assertThat(preference.getPushEnabled()).isFalse();
            assertThat(preference.getEmailEnabled()).isFalse();
            assertThat(preference.getSmsEnabled()).isFalse();
        });
    }

    @Test
    @DisplayName("채널 상태 조회가 알려주는 채널 키는 모두 설정 변경이 받아들인다")
    void 모든_채널_키를_설정_변경이_받아들인다() {
        // 조회는 `inapp` 을 알려주는데 변경은 `inApp` 만 받는 식으로 어긋나면
        // 화면에 보이는 토글이 저장되지 않는다.
        when(userRepository.findByUserId(USER_ID)).thenReturn(Optional.of(user));
        when(preferenceRepository.findByUserAndNotificationType(any(User.class), any()))
                .thenReturn(Optional.empty());
        when(preferenceRepository.save(any(NotificationPreference.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        for (NotificationChannelType channel : NotificationChannelType.values()) {
            assertThatCode(() -> preferenceService.updateChannelPreference(
                    USER_ID, "SYSTEM", channel.getKey(), false))
                    .as("채널 키 %s", channel.getKey())
                    .doesNotThrowAnyException();
        }
    }

    @Test
    @DisplayName("채널 하나만 바꿔도 나머지 채널의 기본값은 그대로 유지된다")
    void 채널_변경이_다른_채널을_켜지_않는다() {
        when(userRepository.findByUserId(USER_ID)).thenReturn(Optional.of(user));
        when(preferenceRepository.findByUserAndNotificationType(user, Notification.NotificationType.HEALTH))
                .thenReturn(Optional.empty());
        when(preferenceRepository.save(any(NotificationPreference.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        NotificationSettingsResponse settings =
                preferenceService.updateChannelPreference(USER_ID, "HEALTH", "push", false);

        assertThat(settings.getPushEnabled()).isFalse();
        assertThat(settings.getEmailEnabled()).isFalse();
        assertThat(settings.getSmsEnabled()).isFalse();
        assertThat(settings.getInAppEnabled()).isTrue();
    }
}
