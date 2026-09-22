package com.carecode.domain.notification.realtime;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * 알림 실시간 채널 (SSE).
 *
 * <p>WebSocket 이 아니라 SSE 를 쓰는 이유: 알림은 서버에서 클라이언트로 가는 한 방향이다. SSE 는 평범한 HTTP 라
 * 기존 JWT 필터·CORS·접근제어가 그대로 적용되고, 프록시·로드밸런서 설정도 따로 필요 없다.
 *
 * <p>한계: 연결을 이 인스턴스 메모리에 둔다. 지금 운영은 단일 인스턴스라 충분하다. 여러 대로 늘리면
 * Redis pub/sub 로 이벤트를 모든 인스턴스에 퍼뜨려야 한다 ({@code docs/features/realtime-notifications.md}).
 */
@Slf4j
@Service
public class NotificationStreamService {

    private final Map<Long, Deque<SseEmitter>> emitters = new ConcurrentHashMap<>();
    private final long timeoutMillis;
    private final int maxConnectionsPerUser;

    public NotificationStreamService(
            @Value("${app.notification.stream.timeout-millis:1800000}") long timeoutMillis,
            @Value("${app.notification.stream.max-connections-per-user:5}") int maxConnectionsPerUser,
            MeterRegistry meterRegistry) {
        this.timeoutMillis = timeoutMillis;
        this.maxConnectionsPerUser = maxConnectionsPerUser;
        Gauge.builder("carecode.notification.stream.connections", this, NotificationStreamService::connectionCount)
                .description("열려 있는 알림 실시간(SSE) 연결 수")
                .register(meterRegistry);
    }

    /**
     * 연결을 연다. 탭을 여러 개 열 수 있으므로 사용자당 여러 연결을 허용하되 상한을 둔다
     * (끊긴 줄 모르고 재연결을 반복하는 클라이언트가 연결을 무한히 쌓지 못하게). 넘치면 가장 오래된 것을 닫는다.
     */
    public SseEmitter connect(Long userDbId) {
        SseEmitter emitter = new SseEmitter(timeoutMillis);
        Deque<SseEmitter> userEmitters = emitters.computeIfAbsent(userDbId, id -> new ConcurrentLinkedDeque<>());
        userEmitters.addLast(emitter);
        while (userEmitters.size() > maxConnectionsPerUser) {
            SseEmitter oldest = userEmitters.pollFirst();
            if (oldest != null) {
                oldest.complete();
            }
        }

        Runnable remove = () -> remove(userDbId, emitter);
        emitter.onCompletion(remove);
        emitter.onTimeout(remove);
        emitter.onError(e -> remove.run());

        // 첫 이벤트를 바로 보내야 프록시가 응답 헤더를 흘려보내고, 클라이언트도 연결됐음을 안다.
        // 재연결 뒤에는 이 이벤트를 신호로 목록을 다시 불러 끊긴 사이의 알림을 채운다.
        send(userDbId, emitter, SseEmitter.event().name("connected").data("ok"));
        return emitter;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onNotificationCreated(NotificationCreatedEvent event) {
        Deque<SseEmitter> userEmitters = emitters.get(event.userDbId());
        if (userEmitters == null || userEmitters.isEmpty()) {
            return;
        }
        for (SseEmitter emitter : List.copyOf(userEmitters)) {
            // id 는 알림 id. 클라이언트는 같은 id 를 두 번 받아도 한 번만 반영한다.
            send(event.userDbId(), emitter, SseEmitter.event()
                    .id(String.valueOf(event.payload().getId()))
                    .name("notification")
                    .data(event.payload(), MediaType.APPLICATION_JSON));
        }
    }

    /** 유휴 연결을 프록시가 끊지 않게 주석 한 줄을 보낸다. 끊긴 연결도 여기서 정리된다. */
    @Scheduled(fixedDelayString = "${app.notification.stream.heartbeat-millis:25000}")
    public void heartbeat() {
        emitters.forEach((userDbId, userEmitters) -> {
            for (SseEmitter emitter : List.copyOf(userEmitters)) {
                send(userDbId, emitter, SseEmitter.event().comment("ping"));
            }
        });
    }

    public int connectionCount() {
        return emitters.values().stream().mapToInt(Deque::size).sum();
    }

    private void send(Long userDbId, SseEmitter emitter, SseEmitter.SseEventBuilder event) {
        try {
            emitter.send(event);
        } catch (IOException | IllegalStateException e) {
            // 클라이언트가 이미 떠났다. 정상 상황이라 경고로 남기지 않는다.
            log.debug("SSE 전송 실패로 연결을 정리합니다 - userDbId={}", userDbId);
            remove(userDbId, emitter);
            emitter.completeWithError(e);
        }
    }

    private void remove(Long userDbId, SseEmitter emitter) {
        emitters.computeIfPresent(userDbId, (id, userEmitters) -> {
            userEmitters.remove(emitter);
            return userEmitters.isEmpty() ? null : userEmitters;
        });
    }
}
