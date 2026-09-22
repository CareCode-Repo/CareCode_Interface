package com.carecode.domain.notification.realtime;

import com.carecode.domain.notification.dto.response.NotificationInfoResponse;
import com.carecode.domain.notification.entity.Notification;
import jakarta.persistence.PostPersist;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 알림이 저장되는 순간을 잡는다.
 *
 * <p>알림을 만드는 곳이 여럿이다 (서비스, 관리자 발송, 빈자리 알림, 초기 데이터 …). 각각에 발송 코드를
 * 넣으면 새 경로가 생길 때 빠뜨린다. 엔티티 저장에 걸면 어느 경로로 만들어도 실시간으로 나간다.
 *
 * <p>여기서는 이벤트만 올린다. 실제 전송은 커밋 뒤({@link NotificationStreamService})에 한다 —
 * 롤백된 알림이 화면에 떴다가 목록에서 사라지는 일을 막는다.
 * 사용자 식별은 DB id 만 쓴다. 지연 로딩 프록시라도 id 는 초기화 없이 읽힌다.
 */
@Component
@RequiredArgsConstructor
public class NotificationPersistListener {

    private final ApplicationEventPublisher publisher;

    @PostPersist
    public void onPersist(Notification notification) {
        if (notification.getUser() == null || notification.getUser().getId() == null) {
            return;
        }
        publisher.publishEvent(new NotificationCreatedEvent(
                notification.getUser().getId(),
                NotificationInfoResponse.builder()
                        .id(notification.getId())
                        .notificationType(notification.getNotificationType() != null
                                ? notification.getNotificationType().name() : null)
                        .title(notification.getTitle())
                        .message(notification.getMessage())
                        .isRead(Boolean.TRUE.equals(notification.getIsRead()))
                        .createdAt(notification.getCreatedAt())
                        .sentAt(notification.getCreatedAt())
                        .build()));
    }
}
