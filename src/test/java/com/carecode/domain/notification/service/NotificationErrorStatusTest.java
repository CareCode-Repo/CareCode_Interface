package com.carecode.domain.notification.service;

import com.carecode.core.exception.BusinessException;
import com.carecode.core.exception.ErrorCode;
import com.carecode.core.exception.ResourceNotFoundException;
import com.carecode.domain.notification.entity.Notification;
import com.carecode.domain.notification.factory.NotificationStrategyFactory;
import com.carecode.domain.notification.repository.NotificationRepository;
import com.carecode.domain.notification.sender.NotificationDispatcher;
import com.carecode.domain.user.entity.User;
import com.carecode.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * 알림 조회가 실패했을 때 <b>누구의 잘못인지</b> 응답이 말해야 한다.
 *
 * <p>{@code getNotificationById} 는 전체를 {@code try} 로 감싸고 {@code catch (Exception e)} 로
 * 받아 {@code CareServiceException} 으로 다시 던졌다. 그 예외는 전역 핸들러에서 코드 문자열로
 * 상태를 정하는데, 코드 없이 던지면 {@code CARE_SERVICE_ERROR} 가 되고 매핑에 없으므로
 * <b>500</b> 이 나간다.
 *
 * <p>그래서 안쪽에서 던진 것이 무엇이든 전부 500 이 됐다.
 *
 * <ul>
 *   <li><b>남의 알림</b> — 소유권 검사가 403 을 던지는데 500 으로 나간다. 클라이언트는 "권한이
 *       없다" 와 "서버가 고장났다" 를 구분할 수 없고, 로그에는 서버 오류로 쌓여 <b>권한 탐색
 *       시도가 장애로 보인다.</b></li>
 *   <li><b>없는 알림</b> — 404 여야 할 것이 500 이다. 클라이언트가 재시도해도 소용없는데
 *       500 은 "다시 해 보라" 는 뜻으로 읽힌다.</li>
 * </ul>
 *
 * <p>이 테스트는 그 두 경우의 상태를 고정한다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("알림 조회 실패의 책임 소재")
class NotificationErrorStatusTest {

    private static final String OWNER = "u-owner";
    private static final String STRANGER = "u-stranger";

    @Mock NotificationRepository notificationRepository;
    @Mock UserRepository userRepository;
    @Mock NotificationStrategyFactory strategyFactory;
    @Mock NotificationDispatcher notificationDispatcher;

    @InjectMocks NotificationService notificationService;

    private Notification ownersNotification;

    @BeforeEach
    void setUp() {
        User owner = User.builder()
                .id(1L).userId(OWNER).email("owner@example.com").name("보호자")
                .build();

        ownersNotification = Notification.builder()
                .id(7L)
                .user(owner)
                .title("예방접종 안내")
                .message("다음 접종일이 다가옵니다")
                .build();
    }

    @Test
    @DisplayName("남의 알림을 요청하면 403 이다 - 500 이면 권한 탐색이 장애로 보인다")
    void readingSomeoneElsesNotificationIsForbidden() {
        when(notificationRepository.findById(7L)).thenReturn(Optional.of(ownersNotification));

        assertThatThrownBy(() -> notificationService.getNotificationById(7L, STRANGER))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.FORBIDDEN));
    }

    @Test
    @DisplayName("없는 알림을 요청하면 404 다 - 500 은 재시도하라는 뜻으로 읽힌다")
    void readingMissingNotificationIsNotFound() {
        when(notificationRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.getNotificationById(404L, OWNER))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
