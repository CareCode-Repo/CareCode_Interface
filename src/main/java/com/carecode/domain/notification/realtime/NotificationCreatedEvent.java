package com.carecode.domain.notification.realtime;

import com.carecode.domain.notification.dto.response.NotificationInfoResponse;

/** 알림 한 건이 저장됐다. 커밋 후 실시간 채널로 내보낸다. */
public record NotificationCreatedEvent(Long userDbId, NotificationInfoResponse payload) {
}
