package com.carecode.domain.notification.app;

import com.carecode.domain.notification.dto.response.NotificationChannelStatusResponse;
import com.carecode.domain.notification.repository.NotificationPreferenceRepository;
import com.carecode.domain.notification.sender.NotificationChannelType;
import com.carecode.domain.notification.sender.NotificationDispatcher;
import com.carecode.domain.notification.service.NotificationPreferenceService;
import com.carecode.domain.notification.service.NotificationService;
import com.carecode.domain.user.entity.User;
import com.carecode.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 채널 상태 조회 검증.
 *
 * <p>발송기가 살아 있어도 이 사용자에게 보낼 주소나 기기가 없으면 알림은 오지 않는다.
 * 설정 화면은 이 응답만 보고 토글을 잠그므로, 두 사정을 모두 반영해야 한다.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotificationChannelStatusTest {

    private static final String USER_ID = "u-1";

    @Mock
    private NotificationService notificationService;

    @Mock
    private NotificationPreferenceService preferenceService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationDispatcher notificationDispatcher;

    @Mock
    private NotificationPreferenceRepository preferenceRepository;

    @InjectMocks
    private NotificationFacade notificationFacade;

    @BeforeEach
    void setUp() {
        // 서버 설정은 모두 정상인 상태에서 시작한다. 수신처 유무만 검증하기 위해서다.
        when(notificationDispatcher.unavailableReason(any())).thenReturn(Optional.empty());
        when(preferenceRepository.findDeviceTokensByUser(any(User.class))).thenReturn(List.of());
    }

    private void givenUser(String email, String phoneNumber) {
        User user = User.builder()
                .id(1L).userId(USER_ID).name("보호자")
                .email(email).phoneNumber(phoneNumber)
                .build();
        when(userRepository.findByUserId(USER_ID)).thenReturn(Optional.of(user));
    }

    private Map<String, NotificationChannelStatusResponse> statuses() {
        return notificationFacade.getChannelStatuses(USER_ID).stream()
                .collect(Collectors.toMap(NotificationChannelStatusResponse::getChannel, Function.identity()));
    }

    @Test
    @DisplayName("인앱은 수신처가 필요 없어 항상 사용할 수 있다")
    void 인앱은_항상_사용할_수_있다() {
        givenUser(null, null);

        assertThat(statuses().get("inapp").isAvailable()).isTrue();
    }

    @Test
    @DisplayName("이메일 주소가 없으면 이메일 채널을 쓸 수 없다")
    void 이메일_주소가_없으면_쓸_수_없다() {
        givenUser(null, "010-0000-0000");

        NotificationChannelStatusResponse email = statuses().get("email");

        assertThat(email.isAvailable()).isFalse();
        assertThat(email.getUnavailableReason()).contains("이메일");
    }

    @Test
    @DisplayName("전화번호가 없으면 문자 채널을 쓸 수 없다")
    void 전화번호가_없으면_문자를_쓸_수_없다() {
        givenUser("parent@example.com", "  ");

        NotificationChannelStatusResponse sms = statuses().get("sms");

        assertThat(sms.isAvailable()).isFalse();
        assertThat(sms.getUnavailableReason()).contains("전화번호");
    }

    @Test
    @DisplayName("등록된 기기가 없으면 푸시를 쓸 수 없다")
    void 등록된_기기가_없으면_푸시를_쓸_수_없다() {
        givenUser("parent@example.com", "010-0000-0000");

        NotificationChannelStatusResponse push = statuses().get("push");

        assertThat(push.isAvailable()).isFalse();
        assertThat(push.getUnavailableReason()).isNotBlank();
        // 사용자가 기기를 등록하면 해결되는 문제다. 화면은 이 코드를 보고 등록 버튼을 띄운다.
        assertThat(push.getReasonCode())
                .isEqualTo(NotificationChannelStatusResponse.REASON_NO_DESTINATION);
    }

    @Test
    @DisplayName("수신처가 모두 있으면 전 채널을 쓸 수 있다")
    void 수신처가_있으면_쓸_수_있다() {
        givenUser("parent@example.com", "010-0000-0000");
        when(preferenceRepository.findDeviceTokensByUser(any(User.class))).thenReturn(List.of("token-1"));

        assertThat(statuses().values())
                .allSatisfy(status -> {
                    assertThat(status.isAvailable()).isTrue();
                    assertThat(status.getUnavailableReason()).isNull();
                });
    }

    @Test
    @DisplayName("서버 설정 문제가 수신처 문제보다 먼저 안내된다")
    void 서버_설정_이유가_우선한다() {
        // 둘 다 문제인 상황에서 "번호를 등록하세요" 라고 안내하면, 등록해도 여전히 안 온다.
        givenUser("parent@example.com", null);
        when(notificationDispatcher.unavailableReason(NotificationChannelType.SMS))
                .thenReturn(Optional.of("문자 발송은 아직 준비 중이에요."));

        NotificationChannelStatusResponse sms = statuses().get("sms");

        assertThat(sms.isAvailable()).isFalse();
        assertThat(sms.getUnavailableReason()).isEqualTo("문자 발송은 아직 준비 중이에요.");
        // 사용자가 번호를 등록해도 해결되지 않는다. 등록을 권해서는 안 된다.
        assertThat(sms.getReasonCode())
                .isEqualTo(NotificationChannelStatusResponse.REASON_SERVER_NOT_CONFIGURED);
    }
}
