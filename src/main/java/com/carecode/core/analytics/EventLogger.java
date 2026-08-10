package com.carecode.core.analytics;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** 행동 이벤트 기록. 지표 수집 실패가 기능을 막지 않도록 비동기로 처리하고 예외를 삼킨다. */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventLogger {

    private static final int MAX_METADATA = 500;

    private final UserEventRepository eventRepository;

    public void log(EventType type, Long userId) {
        log(type, userId, null, null);
    }

    public void log(EventType type, Long userId, String targetId) {
        log(type, userId, targetId, null);
    }

    /** 호출부의 트랜잭션과 분리한다. 이벤트 저장 실패가 본 작업을 롤백시키면 안 된다. */
    @Async("analyticsExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(EventType type, Long userId, String targetId, String metadata) {
        try {
            eventRepository.save(UserEvent.builder()
                    .userId(userId)
                    .eventType(type)
                    .targetId(truncate(targetId, 100))
                    .metadata(truncate(metadata, MAX_METADATA))
                    .occurredAt(LocalDateTime.now())
                    .build());
        } catch (Exception e) {
            log.warn("이벤트 기록 실패 - type={}, 사유={}", type, e.getMessage());
        }
    }

    private String truncate(String value, int limit) {
        if (value == null) {
            return null;
        }
        return value.length() <= limit ? value : value.substring(0, limit);
    }
}
